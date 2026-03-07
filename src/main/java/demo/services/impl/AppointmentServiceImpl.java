package demo.services.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import demo.domains.CancelAppointmentRequest;
import demo.domains.CreateAppointmentRequest;
import demo.domains.ListAppointmentRequest;
import demo.domains.entities.User;
import demo.domains.entities.Appointment;
import demo.domains.entities.AppointmentStatusEnum;
import demo.domains.entities.AppointmentTypeEnum;
import demo.domains.entities.CancellationInitiatorEnum;
import demo.domains.entities.RoleEnum;
import demo.exceptions.UserNotFoundException;
import demo.exceptions.TimeNotInWorkingHourException;
import demo.exceptions.AppointmentNotFoundException;
import demo.exceptions.InvalidAppointmentStatusException;
import demo.exceptions.InvalidConfirmationTimeWindowException;
import demo.exceptions.InvalidCreateAppointmentTimeWindowException;
import demo.exceptions.MismatchedParameterException;
import demo.exceptions.OverlapAppointmentException;
import demo.repositories.UserRepository;
import demo.repositories.AppointmentRepository;
import demo.services.AppointmentService;
import demo.services.BillService;
import demo.specifications.AppointmentSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService{

    private final LocalTime OPENING_TIME = LocalTime.of(8, 0);
    private final LocalTime CLOSING_TIME = LocalTime.of(18, 0);
    private final int MINIMUM_BOOKING_DAY = 3;
    private final int MAXIMUM_BOOKING_DAY = 31;
    private final int QUICK_CHECK_DURATION_MINUTE = 30;
    private final int MID_CHECK_DURATION_MINUTE = 60;
    private final int LONG_CHECK_DURATION_MINUTE = 90;
    private final Duration OVERLAP_BUFFER = Duration.ofMinutes(30);
    private final int LATE_TIME_SOFT_LIMIT_MINUTE = 5;
    private static final int CONFIRMATION_EARLY_WINDOW_MINUTES = 5;
    private static final int CONFIRMATION_LATE_WINDOW_MINUTES = 15;

    private final String DOCTOR_BUSY_CONSTRAINT_NAME = "no_doctor_overlap";
    private final String PATIENT_OVERLAP_APPOINTMENT_CONSTRAINT_NAME = "no_patient_overlap";

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillService billService;

    @Override
    @Transactional
    public Appointment createAppointment(UUID creatorId, CreateAppointmentRequest createAppointmentRequest) {
        
        LocalDateTime startTime = createAppointmentRequest.getStartTime();
        AppointmentTypeEnum type = createAppointmentRequest.getType();
        LocalDateTime endTime = getEndTime(startTime, type);

        checkWithinCreateWindow(startTime);

        checkTimeInWorkingHour(startTime, endTime);

        User creator = userRepository
            .findById(creatorId)
            .orElseThrow(
                () -> new UserNotFoundException(
                    String.format("User with ID %s not found", creatorId)
                )
            );

        UUID doctorId = createAppointmentRequest.getDoctorId();
        User doctor = userRepository
            .findByIdAndRole(doctorId, RoleEnum.DOCTOR)
            .orElseThrow(
                () -> new UserNotFoundException(
                    String.format("Doctor with ID %s not found", doctorId)
                )
            );
        
        UUID patientId = createAppointmentRequest.getPatientId();
        User patient = userRepository
            .findByIdAndRole(patientId, RoleEnum.PATIENT)
            .orElseThrow(
                () -> new UserNotFoundException(
                    String.format("Patient with ID %s not found", patientId)
                )
            );

        /**
         * A race can happen when creating an appointment. Soft check with service logic and Hard check with db constraint
         * 
         * db constraint checks if the doctor is already scheduled with db constraint
         * CREATE EXTENSION IF NOT EXISTS btree_gist;
         * alter table appointments add constraint no_doctor_overlap exclude using gist (doctor_id with =, tsrange(start_time - interval '30 minutes', end_time + interval '30 minutes') with &&);
         * 
         * check if the patient already has appointment in that timeframe with db constraint
         * alter table appointments add constraint no_patient_overlap exclude using gist (patient_id with =, tsrange(start_time - interval '30 minutes', end_time + interval '30 minutes') with &&);
         */

        checkOverlapAppointment(startTime, endTime, doctorId, patientId);

        Appointment appointment = new Appointment();
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setType(type);
        appointment.setStatus(AppointmentStatusEnum.CONFIRMED);
        appointment.setCreator(creator);
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);

        try {
            return appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            // check if the db constrainst is violated
            Throwable cause = ex.getMostSpecificCause();
            
            String message = cause != null ? cause.getMessage() : "";

            if (message.contains(DOCTOR_BUSY_CONSTRAINT_NAME) || message.contains(PATIENT_OVERLAP_APPOINTMENT_CONSTRAINT_NAME)) {
                throw new OverlapAppointmentException();
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public Appointment confirmAppointment(UUID receptionistId, UUID appointmentId) {
        
        LocalDateTime confirmTime = LocalDateTime.now();

        User receptionist = userRepository
            .findById(receptionistId)
            .orElseThrow(
                () -> new UserNotFoundException(
                    String.format("Receptionist with ID %s not found", receptionistId)
                )
            );
        
        Appointment confirmAppointment = appointmentRepository
            .findById(appointmentId)
            .orElseThrow(
                () -> new AppointmentNotFoundException(
                    String.format("Appointment with ID %s not found", appointmentId)
                )
            );

        if (!confirmAppointment.getStatus().equals(AppointmentStatusEnum.CONFIRMED)){
            throw new InvalidAppointmentStatusException(String.format("Invalid status of appoiment with ID %s", appointmentId));
        }

        checkWithinConfirmWindow(confirmAppointment.getStartTime(), confirmTime);

        confirmAppointment.setStatus(AppointmentStatusEnum.COMPLETED);
        confirmAppointment.setConfirmReceptionist(receptionist);
        confirmAppointment.setConfirmedAt(confirmTime);
        // Need to save before generating the bill to avoid racing (racing means two threads may try to confirm the appointment and create two identical bills)
        appointmentRepository.save(confirmAppointment);

        billService.generateBillForAppointment(confirmAppointment);
        
        // TODO: add sending appointment bill to insurance


        if (isLate(confirmTime, confirmAppointment.getStartTime())){
            billService.generateLateFeeBill(confirmAppointment);
                    
            // TODO: add sending appointment bill to insurance

        }

        return confirmAppointment;

    }

    @Override
    public Page<Appointment> listDoctorAppointment(UUID patientId, ListAppointmentRequest request, Pageable pageable) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, patientId);
        roleMap.put(RoleEnum.DOCTOR, request.getDoctorId());
        return listAppointmentHelper(request, roleMap, pageable);
    }

    @Override
    public Page<Appointment> listPatientAppointment(UUID doctorId, ListAppointmentRequest request, Pageable pageable) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, request.getPatientId());
        roleMap.put(RoleEnum.DOCTOR, doctorId);
        return listAppointmentHelper(request, roleMap, pageable);
    }

    @Override
    public Page<Appointment> listConfirmAppointment(UUID receptionistId, ListAppointmentRequest request,Pageable pageable) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, request.getPatientId());
        roleMap.put(RoleEnum.DOCTOR, request.getDoctorId());
        roleMap.put(RoleEnum.RECEPTIONIST, receptionistId);
        return listAppointmentHelper(request, roleMap, pageable);
    }

    @Override
    public Page<Appointment> listAppointment(ListAppointmentRequest request,Pageable pageable) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, request.getPatientId());
        roleMap.put(RoleEnum.DOCTOR, request.getDoctorId());
        return listAppointmentHelper(request, roleMap, pageable);
    }

    @Override
    @Transactional
    public Appointment cancelAppointment(UUID cancellerId, RoleEnum cancellerRole, CancelAppointmentRequest request) {
        
        LocalDateTime cancelAt = LocalDateTime.now();
        
        User canceller = userRepository
            .findById(cancellerId)
            .orElseThrow(
                () -> new UserNotFoundException(
                    String.format("User with ID %s not found", cancellerId)
                )
            );
        UUID appointmentId = request.getAppointmentId();
        Appointment cancelAppointment = appointmentRepository
            .findById(appointmentId)
            .orElseThrow(
                () -> new AppointmentNotFoundException(
                    String.format("Appointment with ID %s not found", appointmentId)
                )
            );

        if (!cancelAppointment.getStatus().equals(AppointmentStatusEnum.CONFIRMED)){
            throw new InvalidAppointmentStatusException(String.format("Invalid status of appoiment with ID %s", appointmentId));
        }

        CancellationInitiatorEnum initiator = request.getCancellationInitiator();
        if (
            (
                CancellationInitiatorEnum.PATIENT.equals(initiator)
                && !cancellerRole.equals(RoleEnum.PATIENT)
            )
            ||
            (
                !CancellationInitiatorEnum.PATIENT.equals(initiator)
                && !cancellerRole.equals(RoleEnum.RECEPTIONIST)
            )
        ){
            throw new MismatchedParameterException();
        }

        cancelAppointment.setCancelledAt(cancelAt);
        cancelAppointment.setCanceller(canceller);
        cancelAppointment.setCancelReason(request.getCancelReason());
        cancelAppointment.setStatus(AppointmentStatusEnum.CANCELLED);
        
        cancelAppointment.setCancellationInitiator(initiator);
        if (
            (
                CancellationInitiatorEnum.RECEPTIONIST_ON_BEHALF_OF_PATIENT.equals(initiator)
                || CancellationInitiatorEnum.PATIENT.equals(initiator)
            )
            && isLateForFreeCancel(cancelAt, cancelAppointment.getStartTime())
        ){
           billService.generateCancellationFeeBill(cancelAppointment);
                   
            // TODO: add sending appointment bill to insurance

        }
        
        return appointmentRepository.save(cancelAppointment);
    }

    private void checkWithinConfirmWindow(LocalDateTime startTime, LocalDateTime confirmTime){
        LocalDateTime min = startTime.minusMinutes(CONFIRMATION_EARLY_WINDOW_MINUTES);
        if (confirmTime.isBefore(min)) throw new InvalidConfirmationTimeWindowException();

        LocalDateTime max = startTime.plusMinutes(CONFIRMATION_LATE_WINDOW_MINUTES);
        if (confirmTime.isAfter(max)) throw new InvalidConfirmationTimeWindowException();
    }
    
    private boolean isLateForFreeCancel(LocalDateTime cancelTime, LocalDateTime startTime){
        return cancelTime.isAfter(startTime.minusDays(MINIMUM_BOOKING_DAY));
    }

    private boolean isLate(LocalDateTime confirmTime, LocalDateTime startTime){
        return confirmTime.isAfter(startTime.plusMinutes(LATE_TIME_SOFT_LIMIT_MINUTE));
    }

    private Page<Appointment> listAppointmentHelper(
        ListAppointmentRequest request,
        Map<RoleEnum, UUID> roleMap,
        Pageable pageable
    ){

        // check for user existance
        roleMap.forEach((role, id) -> {
            if (id!=null){
                userRepository.findByIdAndRole(id, role).orElseThrow(() -> new UserNotFoundException(
                    String.format("%s with ID %s not found", role.name(), id)
                ));
            }
        });

        // parse request
        AppointmentTypeEnum type = request.getType();
        
        AppointmentStatusEnum status = request.getStatus();

        LocalDateTime from = request.getFrom() != null
            ? request.getFrom().atStartOfDay()
            : null;

        LocalDateTime end = request.getEnd() != null
            ? request.getEnd().atTime(LocalTime.MAX)
            : null;

        Specification<Appointment> spec = AppointmentSpecification.alwaysTrue();
        
        // build specification
        UUID patientId = roleMap.get(RoleEnum.PATIENT);
        if (patientId!=null) spec = spec.and(AppointmentSpecification.byPatient(patientId));

        UUID doctorId = roleMap.get(RoleEnum.DOCTOR);
        if (doctorId!=null) spec = spec.and(AppointmentSpecification.byDoctor(doctorId));

        UUID receptionistId = roleMap.get(RoleEnum.RECEPTIONIST);
        if (receptionistId!=null) spec = spec.and(AppointmentSpecification.byReceptionist(receptionistId));

        if (type!=null) spec = spec.and(AppointmentSpecification.byType(type));

        if (status != null) spec = spec.and(AppointmentSpecification.byStatus(status));

        if (from != null || end != null) spec = spec.and(AppointmentSpecification.byDateRange(from, end));

        return appointmentRepository.findAll(spec, pageable);
    }
    
    private void checkOverlapAppointment(LocalDateTime startTime, LocalDateTime endTime, UUID doctorId, UUID patientId){
        LocalDateTime startBufferedTime = startTime.minus(OVERLAP_BUFFER);
        LocalDateTime endBufferedTime = endTime.plus(OVERLAP_BUFFER);

        if (
            appointmentRepository.existsDoctorOverlap(doctorId, startBufferedTime, endBufferedTime)
            || appointmentRepository.existsPatientOverlap(patientId, startBufferedTime, endBufferedTime)
        ){
            throw new OverlapAppointmentException();
        }
    }

    private void checkTimeInWorkingHour(LocalDateTime startTime, LocalDateTime endTime){

        if (startTime.toLocalTime().isBefore(OPENING_TIME)) throw new TimeNotInWorkingHourException();

        if (endTime.toLocalTime().isAfter(CLOSING_TIME)) throw new TimeNotInWorkingHourException();
    
    }

    private void checkWithinCreateWindow(LocalDateTime startTime){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minimum_time = now.plusDays(MINIMUM_BOOKING_DAY);
        if (startTime.isBefore(minimum_time)) throw new InvalidCreateAppointmentTimeWindowException(String.format("Appointment can only be after %s", minimum_time));

        LocalDateTime maximum_time = now.plusDays(MAXIMUM_BOOKING_DAY);
        if (startTime.isAfter(maximum_time)) throw new InvalidCreateAppointmentTimeWindowException(String.format("Appointment can only be before %s", maximum_time));

    }

    private LocalDateTime getEndTime(LocalDateTime startTime, AppointmentTypeEnum type){
        Duration duration;
        switch (type) {
            case QUICK_CHECK:
                duration = Duration.ofMinutes(QUICK_CHECK_DURATION_MINUTE);
                break;
            case MID_CHECK:
                duration = Duration.ofMinutes(MID_CHECK_DURATION_MINUTE);
                break;
            default:
                duration = Duration.ofMinutes(LONG_CHECK_DURATION_MINUTE);
                break;
        }
        return startTime.plus(duration);
    }
}

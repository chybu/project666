package com.project666.backend.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.project666.backend.domain.CancelAppointmentRequest;
import com.project666.backend.domain.CreateAppointmentRequest;
import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.domain.entity.AppointmentTypeEnum;
import com.project666.backend.domain.entity.CancellationInitiatorEnum;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.User;
import com.project666.backend.exception.InvalidConfirmationTimeWindowException;
import com.project666.backend.exception.InvalidCreateAppointmentTimeWindowException;
import com.project666.backend.exception.OverlapAppointmentException;
import com.project666.backend.exception.TimeNotInWorkingHourException;
import com.project666.backend.repository.AppointmentRepository;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.service.AppointmentService;
import com.project666.backend.service.BillService;
import com.project666.backend.specification.AppointmentSpecification;
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
    private final int PRECHECK_DURATION_MINUTE = 15;
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
    public Appointment createAppointmentForPatient(UUID patientId, CreateAppointmentRequest createAppointmentRequest) {
        User patient = requireActiveUserByRole(patientId, RoleEnum.PATIENT);
        return createAppointmentHelper(patient, createAppointmentRequest);
    }

    @Override
    @Transactional
    public Appointment createAppointmentForReceptionist(UUID receptionistId, CreateAppointmentRequest createAppointmentRequest) {
        User receptionist = requireActiveUserByRole(receptionistId, RoleEnum.RECEPTIONIST);
        return createAppointmentHelper(receptionist, createAppointmentRequest);
    }

    @Override
    @Transactional
    public Appointment confirmAppointment(UUID receptionistId, UUID appointmentId) {
        
        LocalDateTime confirmTime = LocalDateTime.now();

        User receptionist = requireActiveUserByRole(receptionistId, RoleEnum.RECEPTIONIST);
        
        Appointment confirmAppointment = appointmentRepository
            .findById(appointmentId)
            .orElseThrow(
                () -> new NoSuchElementException(
                    String.format("Appointment with ID %s not found", appointmentId)
                )
            );

        requireActiveUserByRole(confirmAppointment.getDoctor().getId(), RoleEnum.DOCTOR);
        requireActiveUserByRole(confirmAppointment.getPatient().getId(), RoleEnum.PATIENT);

        if (!confirmAppointment.getStatus().equals(AppointmentStatusEnum.CONFIRMED)){
            throw new IllegalArgumentException(String.format("Invalid status of appoiment with ID %s", appointmentId));
        }

        checkWithinConfirmWindow(confirmAppointment.getStartTime(), confirmTime);

        confirmAppointment.setStatus(AppointmentStatusEnum.COMPLETED);
        confirmAppointment.setConfirmReceptionist(receptionist);
        confirmAppointment.setConfirmedAt(confirmTime);
        // Need to save before generating the bill to avoid racing (racing means two threads may try to confirm the appointment and create two identical bills)
        appointmentRepository.save(confirmAppointment);

        billService.generateBillForAppointment(confirmAppointment);

        if (isLate(confirmTime, confirmAppointment.getStartTime())){
            billService.generateLateFeeBill(confirmAppointment);
        }

        return confirmAppointment;

    }

    @Override
    public Page<Appointment> listAppointmentForPatient(UUID patientId, ListAppointmentRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(patientId, RoleEnum.PATIENT, false));
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(request.getDoctorId(), RoleEnum.DOCTOR, true));
        return listAppointmentHelper(request, userLookupMap, pageable);
    }

    @Override
    public Page<Appointment> listAppointmentForDoctor(UUID doctorId, ListAppointmentRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(request.getPatientId(), RoleEnum.PATIENT, true));
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(doctorId, RoleEnum.DOCTOR, false));
        return listAppointmentHelper(request, userLookupMap, pageable);
    }

    @Override
    public Page<Appointment> listAppointmentForReceptionist(UUID receptionistId, ListAppointmentRequest request,Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(request.getPatientId(), RoleEnum.PATIENT, true));
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(request.getDoctorId(), RoleEnum.DOCTOR, true));
        userLookupMap.put(RoleEnum.RECEPTIONIST, new UserLookup(receptionistId, RoleEnum.RECEPTIONIST, false));
        return listAppointmentHelper(request, userLookupMap, pageable);
    }

    @Override
    public Page<Appointment> listAppointmentForNurse(UUID nurseId, ListAppointmentRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(request.getPatientId(), RoleEnum.PATIENT, true));
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(request.getDoctorId(), RoleEnum.DOCTOR, true));
        userLookupMap.put(RoleEnum.NURSE, new UserLookup(nurseId, RoleEnum.NURSE, false));
        validateUserLookups(userLookupMap.values());

        Specification<Appointment> spec = AppointmentSpecification.alwaysTrue();

        UUID patientId = userLookupMap.get(RoleEnum.PATIENT).id();
        if (patientId != null) {
            spec = spec.and(AppointmentSpecification.byPatient(patientId));
        }

        UUID doctorId = userLookupMap.get(RoleEnum.DOCTOR).id();
        if (doctorId != null) {
            spec = spec.and(AppointmentSpecification.byDoctor(doctorId));
        }

        if (request.getType() != null) {
            spec = spec.and(AppointmentSpecification.byType(request.getType()));
        }

        LocalDateTime now = LocalDateTime.now();
        spec = spec.and(AppointmentSpecification.byStatus(AppointmentStatusEnum.COMPLETED));
        spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("endTime"), now));

        LocalDateTime from = request.getFrom() != null
            ? request.getFrom().atStartOfDay()
            : null;
        LocalDateTime end = request.getEnd() != null
            ? request.getEnd().atTime(LocalTime.MAX)
            : null;

        if (from != null || end != null) {
            if (end != null && from != null && end.isBefore(from)) {
                return Page.empty(pageable);
            }
            spec = spec.and(AppointmentSpecification.byDateRange(from, end));
        }

        return appointmentRepository.findAll(spec, pageable);
    }

    @Override
    public Page<Appointment> searchAnyAppointmentForReceptionist(UUID receptionistId, ListAppointmentRequest request, Pageable pageable) {

        // check if the current receptionist id using this method is valid
        requireActiveUserByRole(receptionistId, RoleEnum.RECEPTIONIST);

        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(request.getPatientId(), RoleEnum.PATIENT, true));
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(request.getDoctorId(), RoleEnum.DOCTOR, true));
        return listAppointmentHelper(request, userLookupMap, pageable);
    }

    @Override
    @Transactional
    public Appointment cancelAppointment(UUID cancellerId, RoleEnum cancellerRole, CancelAppointmentRequest request) {
        
        LocalDateTime cancelAt = LocalDateTime.now();
        
        User canceller = requireActiveUserByRole(cancellerId, cancellerRole);
        UUID appointmentId = request.getAppointmentId();
        Appointment cancelAppointment = appointmentRepository
            .findById(appointmentId)
            .orElseThrow(
                () -> new NoSuchElementException(
                    String.format("Appointment with ID %s not found", appointmentId)
                )
            );

        requireActiveUserByRole(cancelAppointment.getDoctor().getId(), RoleEnum.DOCTOR);
        requireActiveUserByRole(cancelAppointment.getPatient().getId(), RoleEnum.PATIENT);

        if (!cancelAppointment.getStatus().equals(AppointmentStatusEnum.CONFIRMED)){
            throw new IllegalArgumentException(String.format("Invalid status of appoiment with ID %s", appointmentId));
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
            throw new IllegalArgumentException(
                String.format("%s cannot cancel an appointment", initiator.name())
            );
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
        }
        
        return appointmentRepository.save(cancelAppointment);
    }

    @Override
    @Transactional
    public Appointment noShowAppointment(UUID receptionistId, UUID appointmentId){
        
        LocalDateTime noShowAt = LocalDateTime.now();
        
        User receptionist = requireActiveUserByRole(receptionistId, RoleEnum.RECEPTIONIST);
        Appointment noShowAppointment = appointmentRepository
            .findById(appointmentId)
            .orElseThrow(
                () -> new NoSuchElementException(
                    String.format("Appointment with ID %s not found", appointmentId)
                )
            );
        
        if (noShowAt.isBefore(noShowAppointment.getEndTime())) throw new IllegalArgumentException(
            String.format("Appointment ID %s can only marked as no show after %s", noShowAppointment.getId(), noShowAppointment.getEndTime())
        );

        requireActiveUserByRole(noShowAppointment.getDoctor().getId(), RoleEnum.DOCTOR);
        requireActiveUserByRole(noShowAppointment.getPatient().getId(), RoleEnum.PATIENT);

        if (!noShowAppointment.getStatus().equals(AppointmentStatusEnum.CONFIRMED)){
            throw new IllegalArgumentException(String.format("Invalid status of appoiment with ID %s", appointmentId));
        }

        noShowAppointment.setCancelledAt(noShowAt);
        noShowAppointment.setCanceller(receptionist);
        noShowAppointment.setCancelReason("Patient didn't show up");
        noShowAppointment.setStatus(AppointmentStatusEnum.NO_SHOW);
        
        noShowAppointment.setCancellationInitiator(CancellationInitiatorEnum.RECEPTIONIST);

        Appointment saved = appointmentRepository.save(noShowAppointment);
        billService.generateCancellationFeeBill(saved);
        return saved;
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
        Map<RoleEnum, UserLookup> userLookupMap,
        Pageable pageable
    ){
        validateUserLookups(userLookupMap.values());
        
        // build specification
        Specification<Appointment> spec = AppointmentSpecification.alwaysTrue();

        UUID patientId = getLookupId(userLookupMap, RoleEnum.PATIENT);
        if (patientId!=null) spec = spec.and(AppointmentSpecification.byPatient(patientId));

        UUID doctorId = getLookupId(userLookupMap, RoleEnum.DOCTOR);
        if (doctorId!=null) spec = spec.and(AppointmentSpecification.byDoctor(doctorId));

        UUID receptionistId = getLookupId(userLookupMap, RoleEnum.RECEPTIONIST);
        if (receptionistId!=null) spec = spec.and(AppointmentSpecification.byReceptionist(receptionistId));

        AppointmentTypeEnum type = request.getType();
        if (type!=null) spec = spec.and(AppointmentSpecification.byType(type));

        AppointmentStatusEnum status = request.getStatus();
        if (status != null) spec = spec.and(AppointmentSpecification.byStatus(status));

        LocalDateTime from = request.getFrom() != null
            ? request.getFrom().atStartOfDay()
            : null;

        LocalDateTime end = request.getEnd() != null
            ? request.getEnd().atTime(LocalTime.MAX)
            : null;
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

    private void validateCreateAppointmentRequest(CreateAppointmentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create appointment request is required");
        }

        if (request.getStartTime() == null) {
            throw new IllegalArgumentException("Appointment start time is required");
        }

        if (request.getType() == null) {
            throw new IllegalArgumentException("Appointment type is required");
        }

        if (request.getDoctorId() == null) {
            throw new IllegalArgumentException("Appointment doctorId is required");
        }
    }

    private LocalDateTime getEndTime(LocalDateTime startTime, AppointmentTypeEnum type){
        Duration duration;
        Duration precheck_duration = Duration.ofMinutes(PRECHECK_DURATION_MINUTE);
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
        return startTime.plus(duration).plus(precheck_duration);
    }

    private User requireActiveUserByRole(UUID userId, RoleEnum role) {
        return userRepository.findByIdAndRoleAndDeletedFalse(userId, role)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("%s with ID %s not found", role.name(), userId)
            ));
    }

    private void validateUserLookups(Iterable<UserLookup> userLookups) {
        for (UserLookup userLookup : userLookups) {
            if (userLookup == null || userLookup.id() == null) {
                continue;
            }

            boolean exists = userLookup.filter()
                ? userRepository.findByIdAndRole(userLookup.id(), userLookup.role()).isPresent()
                : userRepository.findByIdAndRoleAndDeletedFalse(userLookup.id(), userLookup.role()).isPresent();

            if (!exists) {
                throw new NoSuchElementException(
                    String.format("%s with ID %s not found", userLookup.role().name(), userLookup.id())
                );
            }
        }
    }

    private UUID getLookupId(Map<RoleEnum, UserLookup> userLookupMap, RoleEnum role) {
        UserLookup lookup = userLookupMap.get(role);
        return lookup != null ? lookup.id() : null;
    }

    private record UserLookup(UUID id, RoleEnum role, boolean filter) {
    }

    private Appointment createAppointmentHelper(User creator, CreateAppointmentRequest createAppointmentRequest){
        validateCreateAppointmentRequest(createAppointmentRequest);
        
        LocalDateTime startTime = createAppointmentRequest.getStartTime();
        AppointmentTypeEnum type = createAppointmentRequest.getType();
        LocalDateTime endTime = getEndTime(startTime, type);

        checkWithinCreateWindow(startTime);

        checkTimeInWorkingHour(startTime, endTime);

        UUID doctorId = createAppointmentRequest.getDoctorId();
        User doctor = requireActiveUserByRole(doctorId, RoleEnum.DOCTOR);
        
        User patient;
        UUID patientId;
        if (creator.getRole().equals(RoleEnum.PATIENT)) {
            patient = creator;
            patientId = patient.getId();
        }
        else{
            patientId = createAppointmentRequest.getPatientId();
            patient = requireActiveUserByRole(patientId, RoleEnum.PATIENT);
        }
        
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
}
package com.project666.backend.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.project666.backend.domain.CreatePrescriptionMedicineRequest;
import com.project666.backend.domain.CreatePrescriptionRequest;
import com.project666.backend.domain.ListPrescriptionRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.domain.entity.PatientRecordAccessStatusEnum;
import com.project666.backend.domain.entity.PatientRecordTypeEnum;
import com.project666.backend.domain.entity.Prescription;
import com.project666.backend.domain.entity.PrescriptionMedicine;
import com.project666.backend.domain.entity.PrescriptionStatusEnum;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.User;
import com.project666.backend.exception.MismatchedParameterException;
import com.project666.backend.repository.AppointmentRepository;
import com.project666.backend.repository.PatientRecordAccessRepository;
import com.project666.backend.repository.PrescriptionRepository;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.service.PrescriptionService;
import com.project666.backend.specification.PrescriptionSpecification;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRecordAccessRepository patientRecordAccessRepository;

    @Override
    @Transactional
    public Prescription createPrescription(UUID doctorId, CreatePrescriptionRequest request) {
        User doctor = userRepository.findByIdAndRole(doctorId, RoleEnum.DOCTOR)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("DOCTOR with ID %s not found", doctorId)
            ));

        Appointment appointment = appointmentRepository.findByIdAndDoctorId(request.getAppointmentId(), doctorId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Appointment with ID %s not found", request.getAppointmentId())
            ));


        if (!AppointmentStatusEnum.COMPLETED.equals(appointment.getStatus())) {
            throw new IllegalArgumentException("Cannot create prescription for incomplete appointment");
        }

        validateCreateRequest(request);

        Prescription prescription = new Prescription();
        prescription.setDoctor(doctor);
        prescription.setPatient(appointment.getPatient());
        prescription.setAppointment(appointment);
        prescription.setStatus(PrescriptionStatusEnum.ACTIVE);
        prescription.setStartDate(request.getStartDate());
        prescription.setEndDate(request.getEndDate());
        prescription.setTotalRefills(request.getTotalRefills());
        prescription.setRemainingRefills(request.getTotalRefills());
        prescription.setRefillIntervalDays(request.getRefillIntervalDays());
        prescription.setNextEligibleRefillAt(request.getStartDate().atStartOfDay());
        prescription.setGeneralNote(trimToNull(request.getGeneralNote()));

        for (CreatePrescriptionMedicineRequest medicineRequest : request.getMedicines()) {
            PrescriptionMedicine medicine = new PrescriptionMedicine();
            medicine.setMedicineName(medicineRequest.getMedicineName().trim());
            medicine.setDosage(trimToNull(medicineRequest.getDosage()));
            medicine.setFrequency(trimToNull(medicineRequest.getFrequency()));
            medicine.setRoute(trimToNull(medicineRequest.getRoute()));
            medicine.setInstructions(trimToNull(medicineRequest.getInstructions()));
            medicine.setQuantity(trimToNull(medicineRequest.getQuantity()));
            prescription.addMedicine(medicine);
        }

        return prescriptionRepository.save(prescription);
    }

    @Override
    @Transactional
    public Prescription cancelPrescription(UUID doctorId, UUID prescriptionId) {
        userRepository.findByIdAndRole(doctorId, RoleEnum.DOCTOR)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("DOCTOR with ID %s not found", doctorId)
            ));

        Prescription prescription = prescriptionRepository.findByIdAndDoctorId(prescriptionId, doctorId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Prescription with ID %s not found", prescriptionId)
            ));

        if (PrescriptionStatusEnum.CANCELLED.equals(prescription.getStatus())) {
            throw new IllegalArgumentException("Prescription is already cancelled");
        }

        if (PrescriptionStatusEnum.COMPLETED.equals(prescription.getStatus())) {
            throw new IllegalArgumentException("Cannot cancel a completed prescription");
        }

        prescription.setStatus(PrescriptionStatusEnum.CANCELLED);
        return prescriptionRepository.save(prescription);
    }

    @Override
    @Transactional
    public Prescription consumeRefill(UUID patientId, UUID prescriptionId) {
        userRepository.findByIdAndRole(patientId, RoleEnum.PATIENT)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("PATIENT with ID %s not found", patientId)
            ));

        Prescription prescription = prescriptionRepository.findByIdAndPatientId(prescriptionId, patientId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Prescription with ID %s not found", prescriptionId)
            ));

        LocalDateTime now = LocalDateTime.now();

        if (now.toLocalDate().isAfter(prescription.getEndDate())) {
            prescription.setStatus(PrescriptionStatusEnum.EXPIRED);
            prescriptionRepository.save(prescription);
            throw new IllegalArgumentException("Prescription has expired");
        }

        if (!PrescriptionStatusEnum.ACTIVE.equals(prescription.getStatus())) {
            throw new IllegalArgumentException("Only active prescriptions can consume a refill");
        }

        if (prescription.getRemainingRefills() == null || prescription.getRemainingRefills() <= 0) {
            throw new IllegalArgumentException("Prescription has no refill remaining");
        }

        LocalDateTime nextEligibleRefillAt = prescription.getNextEligibleRefillAt();
        if (nextEligibleRefillAt != null && now.isBefore(nextEligibleRefillAt)) {
            throw new IllegalArgumentException("Prescription refill is not yet eligible");
        }

        int remaining = prescription.getRemainingRefills() - 1;
        prescription.setRemainingRefills(remaining);
        prescription.setLastConsumedAt(now);

        if (remaining <= 0) {
            prescription.setStatus(PrescriptionStatusEnum.COMPLETED);
            prescription.setNextEligibleRefillAt(null);
        } else {
            prescription.setNextEligibleRefillAt(now.plusDays(prescription.getRefillIntervalDays()));
        }

        return prescriptionRepository.save(prescription);
    }

    @Override
    public Page<Prescription> listPrescriptionForDoctor(UUID doctorId, ListPrescriptionRequest request, Pageable pageable) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.DOCTOR, doctorId);
        roleMap.put(RoleEnum.PATIENT, request.getPatientId());
        checkForRoleExistance(roleMap);

        Specification<Prescription> spec = baseSpecification(request);
        spec = spec.and(PrescriptionSpecification.byDoctor(doctorId));

        return prescriptionRepository.findAll(spec, pageable);
    }

    @Override
    public Page<Prescription> listPrescriptionForPatient(UUID patientId, ListPrescriptionRequest request, Pageable pageable) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, patientId);
        roleMap.put(RoleEnum.DOCTOR, request.getDoctorId());
        checkForRoleExistance(roleMap);

        Specification<Prescription> spec = baseSpecification(request);
        spec = spec.and(PrescriptionSpecification.byPatient(patientId));

        return prescriptionRepository.findAll(spec, pageable);
    }

    @Override
    public Page<Prescription> listPrescriptionForNewDoctor(UUID doctorId, ListPrescriptionRequest request, Pageable pageable) {
        userRepository.findByIdAndRole(doctorId, RoleEnum.DOCTOR)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("DOCTOR with ID %s not found", doctorId)
            ));

        List<UUID> approvedPatientIds = patientRecordAccessRepository
            .findPatientIdsByDoctorIdAndRecordTypeAndStatus(
                doctorId,
                PatientRecordTypeEnum.PRESCRIPTION,
                PatientRecordAccessStatusEnum.APPROVED
            );

        if (approvedPatientIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Specification<Prescription> spec = baseSpecification(request);
        spec = spec.and((root, query, cb) -> root.get("patient").get("id").in(approvedPatientIds));
        spec = spec.and((root, query, cb) -> cb.notEqual(root.get("doctor").get("id"), doctorId));

        UUID patientId = request.getPatientId();
        if (patientId != null) {
            userRepository.findByIdAndRole(patientId, RoleEnum.PATIENT)
                .orElseThrow(() -> new NoSuchElementException(
                    String.format("PATIENT with ID %s not found", patientId)
                ));
            spec = spec.and(PrescriptionSpecification.byPatient(patientId));
        }

        return prescriptionRepository.findAll(spec, pageable);
    }

    private Specification<Prescription> baseSpecification(ListPrescriptionRequest request) {
        Specification<Prescription> spec = PrescriptionSpecification.alwaysTrue();

        if (request.getStatus() != null) {
            spec = spec.and(PrescriptionSpecification.byStatus(request.getStatus()));
        }

        if (request.getCreatedAtDate() != null) {
            spec = spec.and(PrescriptionSpecification.byCreatedAtDate(request.getCreatedAtDate()));
        }

        if (request.getAppointmentId() != null) {
            // The appointment filter is still safe without validating ownership here because
            // the final specification is always constrained by the caller identity
            // (doctorId/patientId/approved shared-patient set), so a foreign appointmentId
            // cannot expand visibility beyond records the caller is already allowed to see.
            spec = spec.and(PrescriptionSpecification.byAppointment(request.getAppointmentId()));
        }

        if (request.getDoctorId() != null) {
            spec = spec.and(PrescriptionSpecification.byDoctor(request.getDoctorId()));
        }

        if (request.getMedicineName() != null && !request.getMedicineName().isBlank()) {
            spec = spec.and(PrescriptionSpecification.byMedicineName(request.getMedicineName()));
        }

        return spec;
    }

    private void validateCreateRequest(CreatePrescriptionRequest request) {
        if (request.getMedicines() == null || request.getMedicines().isEmpty()) {
            throw new IllegalArgumentException("Prescription must contain at least one medicine");
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Prescription start date and end date are required");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Prescription end date cannot be before start date");
        }

        if (request.getTotalRefills() == null || request.getTotalRefills() < 0) {
            throw new IllegalArgumentException("Prescription total refills cannot be negative");
        }

        if (request.getRefillIntervalDays() == null || request.getRefillIntervalDays() < 0) {
            throw new IllegalArgumentException("Prescription refill interval cannot be negative");
        }

        for (CreatePrescriptionMedicineRequest medicine : request.getMedicines()) {
            if (medicine.getMedicineName() == null || medicine.getMedicineName().isBlank()) {
                throw new IllegalArgumentException("Prescription medicine name is required");
            }
        }
    }

    private void checkForRoleExistance(Map<RoleEnum, UUID> roleMap) {
        roleMap.forEach((role, id) -> {
            if (id != null) {
                userRepository.findByIdAndRole(id, role).orElseThrow(() -> new NoSuchElementException(
                    String.format("%s with ID %s not found", role.name(), id)
                ));
            }
        });
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

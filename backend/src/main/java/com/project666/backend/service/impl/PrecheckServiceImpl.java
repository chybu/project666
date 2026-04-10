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

import com.project666.backend.domain.CreatePrecheckRequest;
import com.project666.backend.domain.ListPrecheckRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.domain.entity.PatientRecordAccessStatusEnum;
import com.project666.backend.domain.entity.PatientRecordTypeEnum;
import com.project666.backend.domain.entity.Precheck;
import com.project666.backend.domain.entity.PrecheckStatusEnum;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.AppointmentRepository;
import com.project666.backend.repository.PatientRecordAccessRepository;
import com.project666.backend.repository.PrecheckRepository;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.service.PrecheckService;
import com.project666.backend.specification.PrecheckSpecification;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrecheckServiceImpl implements PrecheckService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrecheckRepository precheckRepository;
    private final PatientRecordAccessRepository patientRecordAccessRepository;

    @Override
    @Transactional
    public Precheck createPrecheck(UUID nurseId, CreatePrecheckRequest request) {
        User nurse = requireActiveUserByRole(nurseId, RoleEnum.NURSE);

        validateCreateRequest(request);

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Appointment with ID %s not found", request.getAppointmentId())
            ));

        requireActiveUserByRole(appointment.getPatient().getId(), RoleEnum.PATIENT);
        requireActiveUserByRole(appointment.getDoctor().getId(), RoleEnum.DOCTOR);

        if (!AppointmentStatusEnum.COMPLETED.equals(appointment.getStatus())) {
            throw new IllegalArgumentException("Cannot create precheck for unattended appointment");
        }

        checkWithinAppointmentTimeRange(
            appointment,
            "Precheck can only be created within appointment time range"
        );

        if (precheckRepository.existsByAppointmentIdAndStatus(
            appointment.getId(),
            PrecheckStatusEnum.VALID
        )) {
            throw new IllegalArgumentException("Appointment already has a valid precheck");
        }

        Precheck precheck = new Precheck();
        precheck.setAppointment(appointment);
        precheck.setPatient(appointment.getPatient());
        precheck.setDoctor(appointment.getDoctor());
        precheck.setNurse(nurse);
        precheck.setStatus(PrecheckStatusEnum.VALID);
        precheck.setPulse(request.getPulse());
        precheck.setSugar(request.getSugar());
        precheck.setTemperature(request.getTemperature());
        precheck.setHeight(request.getHeight());
        precheck.setWeight(request.getWeight());
        precheck.setNote(trimToNull(request.getNote()));

        return precheckRepository.save(precheck);
    }

    @Override
    @Transactional
    public Precheck cancelPrecheck(UUID nurseId, UUID precheckId) {
        requireActiveUserByRole(nurseId, RoleEnum.NURSE);

        Precheck precheck = precheckRepository.findByIdAndNurseId(precheckId, nurseId)
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Precheck with ID %s not found", precheckId)
            ));

        requireActiveUserByRole(precheck.getPatient().getId(), RoleEnum.PATIENT);
        requireActiveUserByRole(precheck.getDoctor().getId(), RoleEnum.DOCTOR);

        if (!PrecheckStatusEnum.VALID.equals(precheck.getStatus())) {
            throw new IllegalArgumentException("Only valid precheck can be cancelled");
        }

        checkWithinAppointmentTimeRange(
            precheck.getAppointment(),
            "Precheck can only be cancelled within appointment time range"
        );

        precheck.setStatus(PrecheckStatusEnum.CANCELLED);
        return precheckRepository.save(precheck);
    }

    @Override
    public Page<Precheck> listPrecheckForPatient(UUID patientId, ListPrecheckRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(patientId, RoleEnum.PATIENT, false));
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(request.getDoctorId(), RoleEnum.DOCTOR, true));
        userLookupMap.put(RoleEnum.NURSE, new UserLookup(request.getNurseId(), RoleEnum.NURSE, true));
        return listPrecheckHelper(request, userLookupMap, pageable);
    }

    @Override
    public Page<Precheck> listPrecheckForDoctor(UUID doctorId, ListPrecheckRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(request.getPatientId(), RoleEnum.PATIENT, true));
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(doctorId, RoleEnum.DOCTOR, false));
        userLookupMap.put(RoleEnum.NURSE, new UserLookup(request.getNurseId(), RoleEnum.NURSE, true));
        return listPrecheckHelper(request, userLookupMap, pageable);
    }

    @Override
    public Page<Precheck> listPrecheckForNurse(UUID nurseId, ListPrecheckRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(request.getPatientId(), RoleEnum.PATIENT, true));
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(request.getDoctorId(), RoleEnum.DOCTOR, true));
        userLookupMap.put(RoleEnum.NURSE, new UserLookup(nurseId, RoleEnum.NURSE, false));
        return listPrecheckHelper(request, userLookupMap, pageable);
    }

    @Override
    public Page<Precheck> listSharedPrecheckForDoctor(UUID doctorId, ListPrecheckRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(doctorId, RoleEnum.DOCTOR, false));
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(request.getPatientId(), RoleEnum.PATIENT, true));
        userLookupMap.put(RoleEnum.NURSE, new UserLookup(request.getNurseId(), RoleEnum.NURSE, true));
        validateUserLookups(userLookupMap.values());

        List<UUID> approvedPatientIds = patientRecordAccessRepository
            .findPatientIdsByDoctorIdAndRecordTypeAndStatus(
                doctorId,
                PatientRecordTypeEnum.PRECHECK,
                PatientRecordAccessStatusEnum.APPROVED
            );

        if (approvedPatientIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Specification<Precheck> spec = baseSpecification(request);
        spec = spec.and((root, query, cb) -> root.get("patient").get("id").in(approvedPatientIds));
        spec = spec.and((root, query, cb) -> cb.notEqual(root.get("doctor").get("id"), doctorId));

        if (request.getPatientId() != null) {
            spec = spec.and(PrecheckSpecification.byPatient(request.getPatientId()));
        }

        if (request.getNurseId() != null) {
            spec = spec.and(PrecheckSpecification.byNurse(request.getNurseId()));
        }

        return precheckRepository.findAll(spec, pageable);
    }

    private Page<Precheck> listPrecheckHelper(
        ListPrecheckRequest request,
        Map<RoleEnum, UserLookup> userLookupMap,
        Pageable pageable
    ) {
        validateUserLookups(userLookupMap.values());

        Specification<Precheck> spec = baseSpecification(request);

        UUID patientId = getLookupId(userLookupMap, RoleEnum.PATIENT);
        if (patientId != null) {
            spec = spec.and(PrecheckSpecification.byPatient(patientId));
        }

        UUID doctorId = getLookupId(userLookupMap, RoleEnum.DOCTOR);
        if (doctorId != null) {
            spec = spec.and(PrecheckSpecification.byDoctor(doctorId));
        }

        UUID nurseId = getLookupId(userLookupMap, RoleEnum.NURSE);
        if (nurseId != null) {
            spec = spec.and(PrecheckSpecification.byNurse(nurseId));
        }

        return precheckRepository.findAll(spec, pageable);
    }

    private Specification<Precheck> baseSpecification(ListPrecheckRequest request) {
        Specification<Precheck> spec = PrecheckSpecification.alwaysTrue();

        if (request.getAppointmentId() != null) {
            spec = spec.and(PrecheckSpecification.byAppointment(request.getAppointmentId()));
        }

        if (request.getStatus() != null) {
            spec = spec.and(PrecheckSpecification.byStatus(request.getStatus()));
        }

        if (request.getCreatedAtDate() != null) {
            spec = spec.and(PrecheckSpecification.byCreatedAtDate(request.getCreatedAtDate()));
        }

        return spec;
    }

    private void validateCreateRequest(CreatePrecheckRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        if (request.getAppointmentId() == null) {
            throw new IllegalArgumentException("Appointment ID is required");
        }

        if (request.getPulse() == null || request.getPulse() <= 0) {
            throw new IllegalArgumentException("Pulse must be greater than 0");
        }

        if (request.getSugar() == null || request.getSugar() <= 0) {
            throw new IllegalArgumentException("Sugar must be greater than 0");
        }

        if (request.getTemperature() == null || request.getTemperature() <= 0) {
            throw new IllegalArgumentException("Temperature must be greater than 0");
        }

        if (request.getHeight() == null || request.getHeight() <= 0) {
            throw new IllegalArgumentException("Height must be greater than 0");
        }

        if (request.getWeight() == null || request.getWeight() <= 0) {
            throw new IllegalArgumentException("Weight must be greater than 0");
        }
    }

    private void checkWithinAppointmentTimeRange(Appointment appointment, String message) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(appointment.getStartTime()) || now.isAfter(appointment.getEndTime())) {
            throw new IllegalArgumentException(message);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
}

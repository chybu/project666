package com.project666.backend.service.impl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.project666.backend.domain.ListPatientRecordAccessRequest;
import com.project666.backend.domain.PatientRecordAccessRequest;
import com.project666.backend.domain.entity.PatientRecordAccess;
import com.project666.backend.domain.entity.PatientRecordAccessStatusEnum;
import com.project666.backend.domain.entity.PatientRecordTypeEnum;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.User;
import com.project666.backend.repository.PatientRecordAccessRepository;
import com.project666.backend.repository.UserRepository;
import com.project666.backend.service.PatientRecordAccessService;
import com.project666.backend.specification.PatientRecordAccessSpecification;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientRecordAccessServiceImpl implements PatientRecordAccessService{

    private final UserRepository userRepository;
    private final PatientRecordAccessRepository patientRecordAccessRepository;
    
    @Override
    @Transactional
    public PatientRecordAccess requestPatientRecordAccess(UUID doctorId, PatientRecordAccessRequest request) {
        validatePatientRecordAccessRequest(request);

        User doctor = requireActiveUserByRole(doctorId, RoleEnum.DOCTOR);

        User patient = requireActiveUserByRole(request.getPatientId(), RoleEnum.PATIENT);

        checkDuplicateAccessRequest(request, doctorId);
        
        PatientRecordAccess access = new PatientRecordAccess();
        access.setRecordType(request.getType());
        access.setPatient(patient);
        access.setDoctor(doctor);
        access.setStatus(PatientRecordAccessStatusEnum.PENDING);

        return patientRecordAccessRepository.save(access);
    }

    @Override
    @Transactional
    public PatientRecordAccess approve(UUID patientId, UUID accessId) {
        return setPatientRecordAccessStatus(patientId, RoleEnum.PATIENT, accessId, PatientRecordAccessStatusEnum.APPROVED);
    }

    @Override
    @Transactional
    public PatientRecordAccess deny(UUID patientId, UUID accessId) {
        return setPatientRecordAccessStatus(patientId, RoleEnum.PATIENT, accessId, PatientRecordAccessStatusEnum.REJECTED);
    }

    @Override
    @Transactional
    public PatientRecordAccess revoke(UUID patientId, UUID accessId) {
        return setPatientRecordAccessStatus(patientId, RoleEnum.PATIENT, accessId, PatientRecordAccessStatusEnum.REVOKED);
    }

    @Override
    @Transactional
    public PatientRecordAccess cancel(UUID doctorId, UUID accessId) {
        return setPatientRecordAccessStatus(doctorId, RoleEnum.DOCTOR, accessId, PatientRecordAccessStatusEnum.CANCELLED);
    }

    @Override
    public Page<PatientRecordAccess> listPatientRecordAccess(UUID patientId, ListPatientRecordAccessRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(patientId, RoleEnum.PATIENT, false));
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(request.getDoctorId(), RoleEnum.DOCTOR, true));
        return listPatientRecordAccessHelper(request, userLookupMap, pageable);
    }

    @Override
    public Page<PatientRecordAccess> listSharedPatientRecordAccess(UUID doctorId, ListPatientRecordAccessRequest request, Pageable pageable) {
        Map<RoleEnum, UserLookup> userLookupMap = new HashMap<>();
        userLookupMap.put(RoleEnum.PATIENT, new UserLookup(request.getPatientId(), RoleEnum.PATIENT, true));
        userLookupMap.put(RoleEnum.DOCTOR, new UserLookup(doctorId, RoleEnum.DOCTOR, false));
        return listPatientRecordAccessHelper(request, userLookupMap, pageable);
    }

    private PatientRecordAccess setPatientRecordAccessStatus(UUID userId, RoleEnum role, UUID accessId, PatientRecordAccessStatusEnum status){

        requireActiveUserByRole(userId, role);

        PatientRecordAccess access = null;
        if (RoleEnum.PATIENT.equals(role)){
            access = patientRecordAccessRepository.findByIdAndPatientId(accessId, userId)
                .orElseThrow(() -> new NoSuchElementException(String.format("Patient record access with ID %s not found", accessId)));
            requireActiveUserByRole(access.getDoctor().getId(), RoleEnum.DOCTOR);
        }else if(RoleEnum.DOCTOR.equals(role)){
            access = patientRecordAccessRepository.findByIdAndDoctorId(accessId, userId)
                .orElseThrow(() -> new NoSuchElementException(String.format("Patient record access with ID %s not found", accessId)));
            requireActiveUserByRole(access.getPatient().getId(), RoleEnum.PATIENT);
        }else throw new IllegalArgumentException("only patient/doctor can set access status");

        switch (status) {
            case APPROVED, REJECTED:
                if (access.getStatus()!=PatientRecordAccessStatusEnum.PENDING) throw new IllegalArgumentException(
                    String.format("Cannot approve/reject access request with status %s ", access.getStatus().name())
                );
                break;
            case REVOKED:
                if (access.getStatus()!=PatientRecordAccessStatusEnum.APPROVED) throw new IllegalArgumentException(
                    String.format("Cannot revoke access request with status %s", access.getStatus().name())
                );
                break;
            case CANCELLED:
                if (access.getStatus()!=PatientRecordAccessStatusEnum.PENDING) throw new IllegalArgumentException(
                    String.format("Cannot cancel access request with status %s", access.getStatus().name())
                );
                break;
            case PENDING:
                throw new IllegalArgumentException();
        }
        
        access.setStatus(status);

        return patientRecordAccessRepository.save(access);
    }

    private Page<PatientRecordAccess> listPatientRecordAccessHelper(
        ListPatientRecordAccessRequest request,
        Map<RoleEnum, UserLookup> userLookupMap,
        Pageable pageable
    ){
        validateUserLookups(userLookupMap.values());
        
        // build specification
        Specification<PatientRecordAccess> spec = PatientRecordAccessSpecification.alwaysTrue();

        UUID patientId = getLookupId(userLookupMap, RoleEnum.PATIENT);
        if (patientId!=null) spec = spec.and(PatientRecordAccessSpecification.byPatient(patientId));

        UUID doctorId = getLookupId(userLookupMap, RoleEnum.DOCTOR);
        if (doctorId!=null) spec = spec.and(PatientRecordAccessSpecification.byDoctor(doctorId));

        PatientRecordTypeEnum type = request.getType();
        if (type!=null) spec = spec.and(PatientRecordAccessSpecification.byType(type));

        PatientRecordAccessStatusEnum status = request.getStatus();
        if (status != null) spec = spec.and(PatientRecordAccessSpecification.byStatus(status));

        if (request.getMinDate() != null || request.getMaxDate() != null) {
            spec = spec.and(
                PatientRecordAccessSpecification.byCreatedAtRange(request.getMinDate(), request.getMaxDate())
            );
        }

        LocalDate createdAtDate = request.getCreatedAtDate();
        if (createdAtDate!=null) spec = spec.and(PatientRecordAccessSpecification.byCreatedAtDate(createdAtDate));

        return patientRecordAccessRepository.findAll(spec, pageable);
    }

    private void checkDuplicateAccessRequest(PatientRecordAccessRequest request, UUID doctorId){
        if (patientRecordAccessRepository
            .existsByRecordTypeAndDoctorIdAndPatientIdAndStatusIn(
                request.getType(),
                doctorId,
                request.getPatientId(),
                List.of(
                    PatientRecordAccessStatusEnum.PENDING,
                    PatientRecordAccessStatusEnum.APPROVED
                )
            )
        ) throw new IllegalArgumentException("Duplicated Request");
    }

    private void validatePatientRecordAccessRequest(PatientRecordAccessRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Patient record access request is required");
        }

        if (request.getPatientId() == null) {
            throw new IllegalArgumentException("Patient record access patientId is required");
        }

        if (request.getType() == null) {
            throw new IllegalArgumentException("Patient record access type is required");
        }
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

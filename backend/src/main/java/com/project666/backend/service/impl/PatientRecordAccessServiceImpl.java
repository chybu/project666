package com.project666.backend.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.ListPatientRecordAccessRequest;
import com.project666.backend.domain.PatientRecordAccessRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.domain.entity.AppointmentTypeEnum;
import com.project666.backend.domain.entity.PatientRecordAccess;
import com.project666.backend.domain.entity.PatientRecordAccessStatusEnum;
import com.project666.backend.domain.entity.PatientRecordTypeEnum;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.entity.User;
import com.project666.backend.exception.DuplicateException;
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
        User doctor = userRepository.findByIdAndRole(doctorId, RoleEnum.DOCTOR)
            .orElseThrow(() -> new NoSuchElementException(String.format("Doctor with ID %s not found", doctorId)));

        UUID patientId = request.getPatientId();
        User patient = userRepository.findByIdAndRole(patientId, RoleEnum.PATIENT)
            .orElseThrow(() -> new NoSuchElementException(String.format("Patient with ID %s not found", patientId)));

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
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, patientId);
        roleMap.put(RoleEnum.DOCTOR, request.getDoctorId());
        return listPatientRecordAccessHelper(request, roleMap, pageable);
    }

    @Override
    public Page<PatientRecordAccess> listSharedPatientRecordAccess(UUID doctorId, ListPatientRecordAccessRequest request, Pageable pageable) {
        Map<RoleEnum, UUID> roleMap = new HashMap<>();
        roleMap.put(RoleEnum.PATIENT, request.getPatientId());
        roleMap.put(RoleEnum.DOCTOR, doctorId);
        return listPatientRecordAccessHelper(request, roleMap, pageable);
    }

    private PatientRecordAccess setPatientRecordAccessStatus(UUID userId, RoleEnum role, UUID accessId, PatientRecordAccessStatusEnum status){

        userRepository.findByIdAndRole(userId, role)
            .orElseThrow(() -> new NoSuchElementException(String.format("%s with ID %s not found", role.name(), userId)));

        PatientRecordAccess access = null;
        if (RoleEnum.PATIENT.equals(role)){
            access = patientRecordAccessRepository.findByIdAndPatientId(accessId, userId)
                .orElseThrow(() -> new NoSuchElementException(String.format("Patient record access with ID %s not found", accessId)));
        }else if(RoleEnum.DOCTOR.equals(role)){
            access = patientRecordAccessRepository.findByIdAndDoctorId(accessId, userId)
                .orElseThrow(() -> new NoSuchElementException(String.format("Patient record access with ID %s not found", accessId)));
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
        Map<RoleEnum, UUID> roleMap,
        Pageable pageable
    ){

        // check for user existance
        roleMap.forEach((role, id) -> {
            if (id!=null){
                userRepository.findByIdAndRole(id, role).orElseThrow(() -> new NoSuchElementException(
                    String.format("%s with ID %s not found", role.name(), id)
                ));
            }
        });
        
        // build specification
        Specification<PatientRecordAccess> spec = PatientRecordAccessSpecification.alwaysTrue();

        UUID patientId = roleMap.get(RoleEnum.PATIENT);
        if (patientId!=null) spec = spec.and(PatientRecordAccessSpecification.byPatient(patientId));

        UUID doctorId = roleMap.get(RoleEnum.DOCTOR);
        if (doctorId!=null) spec = spec.and(PatientRecordAccessSpecification.byDoctor(doctorId));

        PatientRecordTypeEnum type = request.getType();
        if (type!=null) spec = spec.and(PatientRecordAccessSpecification.byType(type));

        PatientRecordAccessStatusEnum status = request.getStatus();
        if (status != null) spec = spec.and(PatientRecordAccessSpecification.byStatus(status));

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
        ) throw new DuplicateException("Duplicated Request");
    }
}

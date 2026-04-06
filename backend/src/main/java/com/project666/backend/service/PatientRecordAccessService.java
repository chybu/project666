package com.project666.backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project666.backend.domain.ListPatientRecordAccessRequest;
import com.project666.backend.domain.PatientRecordAccessRequest;
import com.project666.backend.domain.entity.PatientRecordAccess;

public interface PatientRecordAccessService {
    PatientRecordAccess requestPatientRecordAccess(UUID doctorId, PatientRecordAccessRequest request);
    // fixed doctorId, patientId, record type

    PatientRecordAccess approve(UUID patientId, UUID accessId);
    // fixed patientId, accessId

    PatientRecordAccess deny(UUID patientId, UUID accessId);
    // fixed patientId, accessId

    PatientRecordAccess revoke(UUID patientId, UUID accessId);
    // fixed patientId, accessId

    PatientRecordAccess cancel(UUID doctorId, UUID accessId);

    Page<PatientRecordAccess> listPatientRecordAccess(UUID patientId, ListPatientRecordAccessRequest request, Pageable pageable);
    // fixed patientId, doctorId, recordType, status, createdAt

    Page<PatientRecordAccess> listSharedPatientRecordAccess(UUID doctorId, ListPatientRecordAccessRequest request, Pageable pageable);
    // fixed doctorId, patientId, recordType, status, createdAt
}

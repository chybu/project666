package com.project666.backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project666.backend.domain.ListPatientRecordAccessRequest;
import com.project666.backend.domain.PatientRecordAccessRequest;
import com.project666.backend.domain.entity.PatientRecordAccess;

/**
 * Handles requests for doctors to access patient-owned medical record categories.
 */
public interface PatientRecordAccessService {
    /**
     * Creates a new patient record access request from a doctor to a patient.
     */
    PatientRecordAccess requestPatientRecordAccess(UUID doctorId, PatientRecordAccessRequest request);

    /**
     * Approves a pending access request on behalf of the patient.
     */
    PatientRecordAccess approve(UUID patientId, UUID accessId);

    /**
     * Rejects a pending access request on behalf of the patient.
     */
    PatientRecordAccess deny(UUID patientId, UUID accessId);

    /**
     * Revokes a previously approved access request on behalf of the patient.
     */
    PatientRecordAccess revoke(UUID patientId, UUID accessId);

    /**
     * Cancels a pending access request on behalf of the requesting doctor.
     */
    PatientRecordAccess cancel(UUID doctorId, UUID accessId);

    /**
     * Lists record access requests received by a patient.
     */
    Page<PatientRecordAccess> listPatientRecordAccess(UUID patientId, ListPatientRecordAccessRequest request, Pageable pageable);

    /**
     * Lists patient record access grants and requests visible to a doctor.
     */
    Page<PatientRecordAccess> listSharedPatientRecordAccess(UUID doctorId, ListPatientRecordAccessRequest request, Pageable pageable);
}

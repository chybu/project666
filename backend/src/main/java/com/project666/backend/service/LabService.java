package com.project666.backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project666.backend.domain.CreateLabRequestRequest;
import com.project666.backend.domain.ListLabRequestRequest;
import com.project666.backend.domain.ListLabTestRequest;
import com.project666.backend.domain.UpdateLabTestRequest;
import com.project666.backend.domain.dto.PatientLabRequestResponseDto;
import com.project666.backend.domain.entity.LabRequest;
import com.project666.backend.domain.entity.LabTest;

/**
 * Manages lab requests and the lifecycle of individual lab tests.
 */
public interface LabService {
    /**
     * Creates a lab request for a completed appointment and its requested tests.
     */
    LabRequest createLabRequest(UUID doctorId, CreateLabRequestRequest request);

    /**
     * Cancels a doctor's lab request as long as it has not already been completed.
     */
    LabRequest cancelLabRequest(UUID doctorId, UUID requestId);

    /**
     * Lists lab tests assigned to a lab technician, with optional filters.
     */
    Page<LabTest> listLabTestForLabTechnician(UUID labTechnicianId, ListLabTestRequest request, Pageable pageable);

    /**
     * Lists a patient's lab requests while hiding staff-only note fields.
     */
    Page<PatientLabRequestResponseDto> listLabRequestForPatient(UUID patientId, ListLabRequestRequest request, Pageable pageable);

    /**
     * Lists lab requests created by a doctor.
     */
    Page<LabRequest> listLabRequestForDoctor(UUID doctorId, ListLabRequestRequest request, Pageable pageable);

    /**
     * Lists shared lab requests a doctor can view through approved patient record access, excluding their own requests.
     */
    Page<LabRequest> listLabRequestForNewDoctor(UUID doctorId, ListLabRequestRequest request, Pageable pageable);

    /**
     * Lists unfinished lab requests that a lab technician can work on.
     */
    Page<LabRequest> listLabRequestForLabTechnician(UUID labTechnicianId, ListLabRequestRequest request, Pageable pageable);

    /**
     * Assigns a requested lab test to a lab technician and moves it into progress.
     */
    LabTest claimLabTest(UUID labTechnicianId, UUID labTestId);

    /**
     * Updates the editable details and result fields of a lab test in progress.
     */
    LabTest updateLabTest(UUID labTechnicianId, UpdateLabTestRequest request);

    /**
     * Finalizes a lab test, generates its bill, and completes the parent request when all tests are done.
     */
    LabTest submitLabTest(UUID labTechnicianId, UUID labTestId);
}

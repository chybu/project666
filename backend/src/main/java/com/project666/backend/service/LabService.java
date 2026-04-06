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

public interface LabService {
    LabRequest createLabRequest(UUID doctorId, CreateLabRequestRequest request);
    /*
    remember to add this to the db to avoid racing

    CREATE UNIQUE INDEX ux_lab_tests_patient_name_active
    ON lab_tests (patient_id, lower(btrim(name)))
    WHERE status IN ('REQUESTED', 'IN_PROGRESS');
    */

    LabRequest cancelLabRequest(UUID doctorId, UUID requestId);

    Page<LabTest> listLabTestForLabTechnician(UUID labTechnicianId, ListLabTestRequest request, Pageable pageable);

    Page<PatientLabRequestResponseDto> listLabRequestForPatient(UUID patientId, ListLabRequestRequest request, Pageable pageable);

    Page<LabRequest> listLabRequestForDoctor(UUID doctorId, ListLabRequestRequest request, Pageable pageable);

    Page<LabRequest> listLabRequestForLabTechnician(UUID labTechnicianId, ListLabRequestRequest request, Pageable pageable);

    LabTest claimLabTest(UUID labTechnicianId, UUID labTestId);

    LabTest updateLabTest(UUID labTechnicianId, UpdateLabTestRequest request);

    LabTest submitLabTest(UUID labTechnicianId, UUID labTestId);
}

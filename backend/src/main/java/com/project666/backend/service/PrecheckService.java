package com.project666.backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project666.backend.domain.CreatePrecheckRequest;
import com.project666.backend.domain.ListPrecheckRequest;
import com.project666.backend.domain.entity.Precheck;

public interface PrecheckService {

    /**
     * Creates a valid precheck for an attended appointment.
     *
     * SQL to protect against race condition:
     * create unique index uq_prechecks_valid_appointment
     * on prechecks (appointment_id)
     * where status = 'VALID';
     */
    Precheck createPrecheck(UUID nurseId, CreatePrecheckRequest request);

    /**
     * Cancels a nurse's own valid precheck.
     */
    Precheck cancelPrecheck(UUID nurseId, UUID precheckId);

    /**
     * Lists prechecks visible to the patient.
     */
    Page<Precheck> listPrecheckForPatient(UUID patientId, ListPrecheckRequest request, Pageable pageable);

    /**
     * Lists prechecks directly visible to the appointment doctor.
     */
    Page<Precheck> listPrecheckForDoctor(UUID doctorId, ListPrecheckRequest request, Pageable pageable);

    /**
     * Lists prechecks created by the nurse.
     */
    Page<Precheck> listPrecheckForNurse(UUID nurseId, ListPrecheckRequest request, Pageable pageable);

    /**
     * Lists shared prechecks approved for another doctor.
     */
    Page<Precheck> listSharedPrecheckForDoctor(UUID doctorId, ListPrecheckRequest request, Pageable pageable);
}

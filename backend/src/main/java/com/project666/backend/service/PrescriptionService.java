package com.project666.backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project666.backend.domain.CreatePrescriptionRequest;
import com.project666.backend.domain.ListPrescriptionRequest;
import com.project666.backend.domain.entity.Prescription;

public interface PrescriptionService {

    Prescription createPrescription(UUID doctorId, CreatePrescriptionRequest request);

    Prescription cancelPrescription(UUID doctorId, UUID prescriptionId);

    Prescription consumeRefill(UUID patientId, UUID prescriptionId);

    Prescription getPrescriptionForDoctor(UUID doctorId, UUID prescriptionId);

    Page<Prescription> listPrescriptionForDoctor(UUID doctorId, ListPrescriptionRequest request, Pageable pageable);

    Page<Prescription> listPrescriptionForPatient(UUID patientId, ListPrescriptionRequest request, Pageable pageable);

    Page<Prescription> listPrescriptionForNewDoctor(UUID doctorId, ListPrescriptionRequest request, Pageable pageable);
}

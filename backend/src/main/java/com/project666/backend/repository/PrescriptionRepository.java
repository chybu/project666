package com.project666.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.project666.backend.domain.entity.Prescription;

@Repository
public interface PrescriptionRepository extends
    JpaRepository<Prescription, UUID>,
    JpaSpecificationExecutor<Prescription> {

    Optional<Prescription> findByIdAndPatientId(UUID id, UUID patientId);

    Optional<Prescription> findByIdAndDoctorId(UUID id, UUID doctorId);
}

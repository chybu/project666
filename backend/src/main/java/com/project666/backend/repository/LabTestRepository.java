package com.project666.backend.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.project666.backend.domain.entity.LabTest;
import com.project666.backend.domain.entity.LabTestStatusEnum;

@Repository
public interface LabTestRepository extends 
    JpaRepository<LabTest, UUID>,
    JpaSpecificationExecutor<LabTest>
    {
        boolean existsByLabRequestPatientIdAndNameIgnoreCaseAndStatusIn(
            UUID patientId,
            String name,
            Collection<LabTestStatusEnum> statuses
        );

        Optional<LabTest> findByIdAndLabTechnicianId(UUID id, UUID labTechnicianId);
}
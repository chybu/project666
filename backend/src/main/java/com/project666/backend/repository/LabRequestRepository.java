package com.project666.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.project666.backend.domain.entity.LabRequest;

@Repository
public interface LabRequestRepository extends 
    JpaRepository<LabRequest, UUID>,
    JpaSpecificationExecutor<LabRequest>
    {
        Optional<LabRequest> findByIdAndDoctorId(UUID id, UUID doctorId);
}

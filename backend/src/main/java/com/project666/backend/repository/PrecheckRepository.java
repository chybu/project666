package com.project666.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.project666.backend.domain.entity.Precheck;
import com.project666.backend.domain.entity.PrecheckStatusEnum;

@Repository
public interface PrecheckRepository extends
    JpaRepository<Precheck, UUID>,
    JpaSpecificationExecutor<Precheck> {

    Optional<Precheck> findByIdAndNurseId(UUID precheckId, UUID nurseId);

    Optional<Precheck> findByIdAndPatientId(UUID precheckId, UUID patientId);

    Optional<Precheck> findByIdAndDoctorId(UUID precheckId, UUID doctorId);

    boolean existsByAppointmentIdAndStatus(UUID appointmentId, PrecheckStatusEnum status);
}

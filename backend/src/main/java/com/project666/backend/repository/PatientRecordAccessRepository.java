package com.project666.backend.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.project666.backend.domain.entity.PatientRecordAccess;
import com.project666.backend.domain.entity.PatientRecordAccessStatusEnum;
import com.project666.backend.domain.entity.PatientRecordTypeEnum;

@Repository
public interface PatientRecordAccessRepository extends
    JpaRepository<PatientRecordAccess, UUID>,
    JpaSpecificationExecutor<PatientRecordAccess>
    {
        Optional<PatientRecordAccess> findByIdAndPatientId(UUID accessId, UUID patientId);

        Optional<PatientRecordAccess> findByIdAndDoctorId(UUID id, UUID doctorId);

        boolean existsByRecordTypeAndDoctorIdAndPatientIdAndStatusIn(
            PatientRecordTypeEnum recordType,
            UUID doctorId,
            UUID patientId,
            Collection<PatientRecordAccessStatusEnum> statuses
        );
}

package com.project666.backend.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project666.backend.domain.entity.Appointment;

@Repository
public interface AppointmentRepository extends 
    JpaRepository<Appointment, UUID>,
    JpaSpecificationExecutor<Appointment>
    {

    @Query("""
        select (count(a)>0)
        from Appointment a
        where a.doctor.id = :doctorId
            and a.endTime > :startTime
            and a.startTime < :endTime
    """)
    boolean existsDoctorOverlap(
        @Param("doctorId") UUID doctorId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        select (count(a)>0)
        from Appointment a
        where a.patient.id = :patientId
            and a.endTime > :startTime
            and a.startTime < :endTime
    """)
    boolean existsPatientOverlap(
        @Param("patientId") UUID patientId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    Optional<Appointment> findByIdAndDoctorId(UUID id, UUID doctorId);
}

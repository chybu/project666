package com.project666.backend.specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.project666.backend.domain.entity.Prescription;
import com.project666.backend.domain.entity.PrescriptionStatusEnum;

import jakarta.persistence.criteria.JoinType;

public final class PrescriptionSpecification {

    private PrescriptionSpecification() {
    }

    public static Specification<Prescription> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Prescription> byPatient(UUID patientId) {
        return (root, query, cb) ->
            patientId == null ? null :
            cb.equal(root.get("patient").get("id"), patientId);
    }

    public static Specification<Prescription> byDoctor(UUID doctorId) {
        return (root, query, cb) ->
            doctorId == null ? null :
            cb.equal(root.get("doctor").get("id"), doctorId);
    }

    public static Specification<Prescription> byAppointment(UUID appointmentId) {
        return (root, query, cb) ->
            appointmentId == null ? null :
            cb.equal(root.get("appointment").get("id"), appointmentId);
    }

    public static Specification<Prescription> byStatus(PrescriptionStatusEnum status) {
        return (root, query, cb) ->
            status == null ? null :
            cb.equal(root.get("status"), status);
    }

    public static Specification<Prescription> overlappingMinDate(LocalDate minDate) {
        return (root, query, cb) ->
            minDate == null ? null :
            cb.greaterThanOrEqualTo(root.get("endDate"), minDate);
    }

    public static Specification<Prescription> overlappingMaxDate(LocalDate maxDate) {
        return (root, query, cb) ->
            maxDate == null ? null :
            cb.lessThanOrEqualTo(root.get("startDate"), maxDate);
    }

    public static Specification<Prescription> byStartDate(LocalDate startDate) {
        return (root, query, cb) ->
            startDate == null ? null :
            cb.equal(root.get("startDate"), startDate);
    }

    public static Specification<Prescription> byEndDate(LocalDate endDate) {
        return (root, query, cb) ->
            endDate == null ? null :
            cb.equal(root.get("endDate"), endDate);
    }

    public static Specification<Prescription> byRemainingRefills(Integer remainingRefills) {
        return (root, query, cb) ->
            remainingRefills == null ? null :
            cb.equal(root.get("remainingRefills"), remainingRefills);
    }

    public static Specification<Prescription> byCreatedAtDate(LocalDate createdAtDate) {
        return (root, query, cb) -> {
            if (createdAtDate == null) return null;

            LocalDateTime start = createdAtDate.atStartOfDay();
            LocalDateTime end = createdAtDate.plusDays(1).atStartOfDay();

            return cb.and(
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)
            );
        };
    }

    public static Specification<Prescription> byMedicineName(String medicineName) {
        return (root, query, cb) -> {
            if (medicineName == null || medicineName.isBlank()) {
                return null;
            }

            if (query != null) {
                query.distinct(true);
            }

            return cb.like(
                cb.lower(root.join("medicines", JoinType.LEFT).get("medicineName")),
                "%" + medicineName.trim().toLowerCase() + "%"
            );
        };
    }
}

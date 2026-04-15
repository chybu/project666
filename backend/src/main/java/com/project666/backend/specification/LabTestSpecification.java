package com.project666.backend.specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.project666.backend.domain.entity.LabTest;
import com.project666.backend.domain.entity.LabTestStatusEnum;

public final class LabTestSpecification {

    private LabTestSpecification(){
    }

    public static Specification<LabTest> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<LabTest> byDoctor(UUID doctorId) {
        return (root, query, cb) ->
            doctorId == null ? null :
            cb.equal(root.get("labRequest").get("doctor").get("id"), doctorId);
    }

    public static Specification<LabTest> byPatient(UUID patientId) {
        return (root, query, cb) ->
            patientId == null ? null :
            cb.equal(root.get("labRequest").get("patient").get("id"), patientId);
    }

    public static Specification<LabTest> byLabTechnician(UUID labTechnicianId) {
        return (root, query, cb) ->
            labTechnicianId == null ? null :
            cb.equal(root.get("labTechnician").get("id"), labTechnicianId);
    }

    public static Specification<LabTest> byStatus(LabTestStatusEnum status) {
        return (root, query, cb) ->
            status == null ? null :
            cb.equal(root.get("status"), status);
    }

    public static Specification<LabTest> byCode(String code) {
        return (root, query, cb) ->
            code == null || code.isBlank() ? null :
            cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%");
    }

    public static Specification<LabTest> byName(String name) {
        return (root, query, cb) ->
            name == null || name.isBlank() ? null :
            cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<LabTest> byUnit(String unit) {
        return (root, query, cb) ->
            unit == null || unit.isBlank() ? null :
            cb.like(cb.lower(root.get("unit")), "%" + unit.toLowerCase() + "%");
    }

    public static Specification<LabTest> byCreatedAtDate(LocalDate createdAtDate) {
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

    public static Specification<LabTest> byCreatedAtRange(LocalDate minDate, LocalDate maxDate) {
        return (root, query, cb) -> {
            if (minDate == null && maxDate == null) return null;

            LocalDateTime start = minDate != null ? minDate.atStartOfDay() : null;
            LocalDateTime endExclusive = maxDate != null ? maxDate.plusDays(1).atStartOfDay() : null;

            if (start != null && endExclusive != null) {
                return cb.and(
                    cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                    cb.lessThan(root.get("createdAt"), endExclusive)
                );
            }

            if (start != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), start);
            }

            return cb.lessThan(root.get("createdAt"), endExclusive);
        };
    }
}

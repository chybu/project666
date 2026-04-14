package com.project666.backend.specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.project666.backend.domain.entity.PatientRecordAccess;
import com.project666.backend.domain.entity.PatientRecordAccessStatusEnum;
import com.project666.backend.domain.entity.PatientRecordTypeEnum;

public final class PatientRecordAccessSpecification {

    private PatientRecordAccessSpecification(){
    }

    public static Specification<PatientRecordAccess> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<PatientRecordAccess> byPatient(UUID patientId) {
        return (root, query, cb) ->
            patientId == null ? null :
            cb.equal(root.get("patient").get("id"), patientId);
    }

    public static Specification<PatientRecordAccess> byDoctor(UUID doctorId) {
        return (root, query, cb) ->
            doctorId == null ? null :
            cb.equal(root.get("doctor").get("id"), doctorId);
    }

    public static Specification<PatientRecordAccess> byType(PatientRecordTypeEnum type) {
        return (root, query, cb) ->
            type == null ? null :
            cb.equal(root.get("recordType"), type);
    }

    public static Specification<PatientRecordAccess> byStatus(PatientRecordAccessStatusEnum status) {
        return (root, query, cb) ->
            status == null ? null :
            cb.equal(root.get("status"), status);
    }

    public static Specification<PatientRecordAccess> byCreatedAtDate(LocalDate createdAtDate) {
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

    public static Specification<PatientRecordAccess> byCreatedAtRange(LocalDate minDate, LocalDate maxDate) {
        return (root, query, cb) -> {
            if (minDate == null && maxDate == null) {
                return null;
            }

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

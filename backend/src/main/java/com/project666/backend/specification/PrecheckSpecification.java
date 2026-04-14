package com.project666.backend.specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.project666.backend.domain.entity.AppointmentTypeEnum;
import com.project666.backend.domain.entity.Precheck;
import com.project666.backend.domain.entity.PrecheckStatusEnum;

public final class PrecheckSpecification {

    private PrecheckSpecification() {
    }

    public static Specification<Precheck> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Precheck> byAppointment(UUID appointmentId) {
        return (root, query, cb) ->
            appointmentId == null ? null :
            cb.equal(root.get("appointment").get("id"), appointmentId);
    }

    public static Specification<Precheck> byPatient(UUID patientId) {
        return (root, query, cb) ->
            patientId == null ? null :
            cb.equal(root.get("patient").get("id"), patientId);
    }

    public static Specification<Precheck> byDoctor(UUID doctorId) {
        return (root, query, cb) ->
            doctorId == null ? null :
            cb.equal(root.get("doctor").get("id"), doctorId);
    }

    public static Specification<Precheck> byNurse(UUID nurseId) {
        return (root, query, cb) ->
            nurseId == null ? null :
            cb.equal(root.get("nurse").get("id"), nurseId);
    }

    public static Specification<Precheck> byStatus(PrecheckStatusEnum status) {
        return (root, query, cb) ->
            status == null ? null :
            cb.equal(root.get("status"), status);
    }

    public static Specification<Precheck> byAppointmentType(AppointmentTypeEnum type) {
        return (root, query, cb) ->
            type == null ? null :
            cb.equal(root.get("appointment").get("type"), type);
    }

    public static Specification<Precheck> byCreatedAtDate(LocalDate createdAtDate) {
        return (root, query, cb) -> {
            if (createdAtDate == null) {
                return null;
            }

            LocalDateTime start = createdAtDate.atStartOfDay();
            LocalDateTime end = createdAtDate.plusDays(1).atStartOfDay();

            return cb.and(
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)
            );
        };
    }

    public static Specification<Precheck> byCreatedAtRange(LocalDate minDate, LocalDate maxDate) {
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

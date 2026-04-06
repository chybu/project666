package com.project666.backend.specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.project666.backend.domain.entity.LabRequest;
import com.project666.backend.domain.entity.LabRequestStatusEnum;

public class LabRequestSpecification {

    public static Specification<LabRequest> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<LabRequest> byStatus(LabRequestStatusEnum status) {
        return (root, query, cb) ->
            status == null ? null :
            cb.equal(root.get("status"), status);
    }

    public static Specification<LabRequest> byCreatedAtDate(LocalDate createdAtDate) {
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

    public static Specification<LabRequest> byDoctor(UUID doctorId) {
        return (root, query, cb) ->
            doctorId == null ? null :
            cb.equal(root.get("doctor").get("id"), doctorId);
    }

    public static Specification<LabRequest> byPatient(UUID patientId) {
        return (root, query, cb) ->
            patientId == null ? null :
            cb.equal(root.get("patient").get("id"), patientId);
    }

    public static Specification<LabRequest> byAppointment(UUID appointmentId) {
        return (root, query, cb) ->
            appointmentId == null ? null :
            cb.equal(root.get("appointment").get("id"), appointmentId);
    }

    public static Specification<LabRequest> unfinishedOnly() {
        return (root, query, cb) ->
            root.get("status").in(
                LabRequestStatusEnum.REQUESTED,
                LabRequestStatusEnum.IN_PROGRESS
            );
    }
}

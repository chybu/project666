package com.project666.backend.specification;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.domain.entity.AppointmentTypeEnum;

public final class AppointmentSpecification {

    private AppointmentSpecification(){
    }

    // This is CriteriaBuilder

    public static Specification<Appointment> alwaysTrue(){
        return (root, query, cb) ->
            cb.conjunction();
    }

    public static Specification<Appointment> byPatient(UUID patientId){
        return (root, query, cb) -> 
            patientId == null ? null :
            cb.equal(root.get("patient").get("id"), patientId);
    }

    public static Specification<Appointment> byDoctor(UUID doctorId){
        return (root, query, cb) -> 
            doctorId == null ? null :
            cb.equal(root.get("doctor").get("id"), doctorId);
    }

    public static Specification<Appointment> byReceptionist(UUID receptionistId){
        return (root, query, cb) -> 
            receptionistId == null ? null :
            cb.equal(root.get("confirmReceptionist").get("id"), receptionistId);
    }

    public static Specification<Appointment> byType(AppointmentTypeEnum type){
        return (root, query, cb) -> 
            type == null ? null :
            cb.equal(root.get("type"), type);
    }

    public static Specification<Appointment> byStatus(AppointmentStatusEnum status){
        return (root, query, cb) -> 
            status == null ? null :
            cb.equal(root.get("status"), status);
    }

    public static Specification<Appointment> byStatuses(Collection<AppointmentStatusEnum> statuses) {
        return (root, query, cb) ->
            statuses == null || statuses.isEmpty() ? null :
            root.get("status").in(statuses);
    }

    public static Specification<Appointment> withoutValidPrecheckByOtherNurse(UUID nurseId) {
        return (root, query, cb) -> {
            if (nurseId == null) {
                return null;
            }

            query.distinct(true);

            var subquery = query.subquery(UUID.class);
            var precheckRoot = subquery.from(com.project666.backend.domain.entity.Precheck.class);
            subquery.select(precheckRoot.get("appointment").get("id"));
            subquery.where(
                cb.equal(precheckRoot.get("appointment").get("id"), root.get("id")),
                cb.equal(precheckRoot.get("status"), com.project666.backend.domain.entity.PrecheckStatusEnum.VALID),
                cb.notEqual(precheckRoot.get("nurse").get("id"), nurseId)
            );

            return cb.not(cb.exists(subquery));
        };
    }

    public static Specification<Appointment> byDateRange(
        LocalDateTime from,
        LocalDateTime end
    ){
        return (root, query, cb) -> {
            if (from == null && end == null) return null;

            if (from != null && end != null){
                return cb.between(root.get("startTime"), from, end);
            }

            if (from != null){
                return cb.greaterThanOrEqualTo(root.get("startTime"), from);
            }

            return cb.lessThanOrEqualTo(root.get("startTime"), end);
        };
    }
    
}

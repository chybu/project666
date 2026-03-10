package demo.specification;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import demo.domain.entities.Appointment;
import demo.domain.entities.AppointmentStatusEnum;
import demo.domain.entities.AppointmentTypeEnum;

public class AppointmentSpecification {

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

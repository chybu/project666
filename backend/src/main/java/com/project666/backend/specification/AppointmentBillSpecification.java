package com.project666.backend.specification;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.project666.backend.domain.entity.AppointmentBill;

public final class AppointmentBillSpecification {

    private AppointmentBillSpecification() {
    }

    public static Specification<AppointmentBill> byAppointment(UUID appointmentId) {
        return (root, query, cb) ->
            appointmentId == null ? null :
            cb.equal(root.get("appointment").get("id"), appointmentId);
    }
}

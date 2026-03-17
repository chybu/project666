package com.project666.backend.service;

import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentBill;

public interface BillService {
    AppointmentBill generateBillForAppointment(Appointment appointment);

    AppointmentBill generateLateFeeBill(Appointment appointment);

    AppointmentBill generateCancellationFeeBill(Appointment appointment);
}

package demo.service;

import demo.domain.entities.Appointment;
import demo.domain.entities.AppointmentBill;

public interface BillService {
    AppointmentBill generateBillForAppointment(Appointment appointment);

    AppointmentBill generateLateFeeBill(Appointment appointment);

    AppointmentBill generateCancellationFeeBill(Appointment appointment);
}

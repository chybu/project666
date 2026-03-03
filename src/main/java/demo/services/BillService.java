package demo.services;

import demo.domains.entities.Appointment;
import demo.domains.entities.AppointmentBill;

public interface BillService {
    AppointmentBill generateBillForAppointment(Appointment appointment);

    AppointmentBill generateLateFeeBill(Appointment appointment);

    AppointmentBill generateCancellationFeeBill(Appointment appointment);
}

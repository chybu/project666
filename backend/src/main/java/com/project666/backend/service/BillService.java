package com.project666.backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project666.backend.domain.ListAppointmentBillRequest;
import com.project666.backend.domain.ListLabBillRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.AppointmentBill;
import com.project666.backend.domain.entity.BaseBill;
import com.project666.backend.domain.entity.LabBill;
import com.project666.backend.domain.entity.LabTest;

/**
 * Creates and manages appointment and lab billing records.
 */
public interface BillService {
    /**
     * Creates the base visit bill for a completed appointment.
     */
    AppointmentBill generateBillForAppointment(Appointment appointment);

    /**
     * Creates the extra fee bill for a patient who arrives late to an appointment.
     */
    AppointmentBill generateLateFeeBill(Appointment appointment);

    /**
     * Creates the cancellation fee bill when a cancelled appointment is no longer eligible for free cancellation.
     */
    AppointmentBill generateCancellationFeeBill(Appointment appointment);

    /**
     * Creates the bill for a completed lab test.
     */
    LabBill generateBillForLabTest(LabTest labTest);

    /**
     * Lists appointment bills that belong to a patient.
     */
    Page<AppointmentBill> listAppointmentBillForPatient(UUID patientId, ListAppointmentBillRequest request, Pageable pageable);

    /**
     * Lists appointment bills assigned to an accountant for payment confirmation.
     */
    Page<AppointmentBill> listAppointmentBillForAccountant(UUID accountantId, ListAppointmentBillRequest request, Pageable pageable);

    /**
     * Lets an accountant search appointment bills across patients and confirming accountants.
     */
    Page<AppointmentBill> searchAnyAppointmentBillForAccountant(
        UUID accountantId,
        ListAppointmentBillRequest request,
        Pageable pageable
    );

    /**
     * Lists lab bills that belong to a patient.
     */
    Page<LabBill> listLabBillForPatient(UUID patientId, ListLabBillRequest request, Pageable pageable);

    /**
     * Lists lab bills assigned to an accountant for payment confirmation.
     */
    Page<LabBill> listLabBillForAccountant(UUID accountantId, ListLabBillRequest request, Pageable pageable);

    /**
     * Lets an accountant search lab bills across patients and confirming accountants.
     */
    Page<LabBill> searchAnyLabBillForAccountant(
        UUID accountantId,
        ListLabBillRequest request,
        Pageable pageable
    );

    /**
     * Marks an appointment bill or lab bill as paid.
     */
    BaseBill confirmBillPayment(UUID accountantId, UUID billId);
}

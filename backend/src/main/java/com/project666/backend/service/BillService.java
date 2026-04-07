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

public interface BillService {
    AppointmentBill generateBillForAppointment(Appointment appointment);

    AppointmentBill generateLateFeeBill(Appointment appointment);

    AppointmentBill generateCancellationFeeBill(Appointment appointment);

    LabBill generateBillForLabTest(LabTest labTest);

    Page<AppointmentBill> listAppointmentBillForPatient(UUID patientId, ListAppointmentBillRequest request, Pageable pageable);

    Page<AppointmentBill> listAppointmentBillForAccountant(UUID accountantId, ListAppointmentBillRequest request, Pageable pageable);

    Page<AppointmentBill> searchAnyAppointmentBillForAccountant(
        UUID accountantId,
        ListAppointmentBillRequest request,
        Pageable pageable
    );

    Page<LabBill> listLabBillForPatient(UUID patientId, ListLabBillRequest request, Pageable pageable);

    Page<LabBill> listLabBillForAccountant(UUID accountantId, ListLabBillRequest request, Pageable pageable);

    Page<LabBill> searchAnyLabBillForAccountant(
        UUID accountantId,
        ListLabBillRequest request,
        Pageable pageable
    );

    BaseBill confirmBillPayment(UUID accountantId, UUID billId);
}
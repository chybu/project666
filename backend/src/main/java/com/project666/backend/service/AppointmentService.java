package com.project666.backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project666.backend.domain.CancelAppointmentRequest;
import com.project666.backend.domain.CreateAppointmentRequest;
import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.RoleEnum;

public interface AppointmentService {
    Appointment createAppointment(UUID creatorId, CreateAppointmentRequest request);

    Appointment confirmAppointment(UUID receptionistId, UUID appointmentId);
    
    Appointment cancelAppointment(UUID cancellerId, RoleEnum cancellerRole, CancelAppointmentRequest request);

    Appointment noShowAppointment(UUID staffId, UUID appointmentId);

    Page<Appointment> listDoctorAppointment(UUID patientId, ListAppointmentRequest request, Pageable pageable);

    Page<Appointment> listPatientAppointment(UUID doctorId, ListAppointmentRequest request, Pageable pageable);

    Page<Appointment> listConfirmAppointment(UUID receptionistId, ListAppointmentRequest request, Pageable pageable);

    Page<Appointment> listAppointment(ListAppointmentRequest request, Pageable pageable);
}

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
    /**
     * Creates a confirmed appointment.
     */
    Appointment createAppointment(UUID creatorId, CreateAppointmentRequest request);

    /**
     * Confirms patient check-in for an appointment.
     */
    Appointment confirmAppointment(UUID receptionistId, UUID appointmentId);

    /**
     * Lists appointments visible to the patient.
     */
    Page<Appointment> listAppointmentForPatient(UUID patientId, ListAppointmentRequest request, Pageable pageable);

    /**
     * Lists appointments assigned to the doctor.
     */
    Page<Appointment> listAppointmentForDoctor(UUID doctorId, ListAppointmentRequest request, Pageable pageable);

    /**
     * Lists appointments tied to the receptionist.
     */
    Page<Appointment> listAppointmentForReceptionist(UUID receptionistId, ListAppointmentRequest request, Pageable pageable);

    /**
     * Lists upcoming confirmed appointments for nurses.
     */
    Page<Appointment> listAppointmentForNurse(UUID nurseId, ListAppointmentRequest request, Pageable pageable);

    /**
     * Searches appointments across the system for receptionist.
     */
    Page<Appointment> searchAnyAppointmentForReceptionist(UUID receptionistId, ListAppointmentRequest request, Pageable pageable);

    /**
     * Cancels a confirmed appointment.
     */
    Appointment cancelAppointment(UUID cancellerId, RoleEnum cancellerRole, CancelAppointmentRequest request);
}

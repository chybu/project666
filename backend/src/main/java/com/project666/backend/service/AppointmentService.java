package com.project666.backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project666.backend.domain.CancelAppointmentRequest;
import com.project666.backend.domain.CreateAppointmentRequest;
import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.RoleEnum;

/**
 * Handles appointment booking, confirmation, search, and cancellation flows.
 */
public interface AppointmentService {
    /**
     * Creates a confirmed appointment for the given doctor, patient, and time slot.
     */
    Appointment createAppointment(UUID creatorId, CreateAppointmentRequest request);

    /**
     * Marks a scheduled appointment as completed when the patient checks in within the allowed time window.
     */
    Appointment confirmAppointment(UUID receptionistId, UUID appointmentId);

    /**
     * Lists appointments visible to a patient, with optional filtering.
     */
    Page<Appointment> listAppointmentForPatient(UUID patientId, ListAppointmentRequest request, Pageable pageable);

    /**
     * Lists appointments assigned to a doctor, with optional filtering.
     */
    Page<Appointment> listAppointmentForDoctor(UUID doctorId, ListAppointmentRequest request, Pageable pageable);

    /**
     * Lists appointments tied to a receptionist's work, with optional filtering.
     */
    Page<Appointment> listAppointmentForReceptionist(UUID receptionistId, ListAppointmentRequest request, Pageable pageable);

    /**
     * Lets a receptionist search appointments across the system, including ones not created or confirmed by them.
     */
    Page<Appointment> searchAnyAppointmentForReceptionist(UUID receptionistId, ListAppointmentRequest request, Pageable pageable);

    /**
     * Cancels a confirmed appointment and applies any required cancellation fee rules.
     */
    Appointment cancelAppointment(UUID cancellerId, RoleEnum cancellerRole, CancelAppointmentRequest request);
}

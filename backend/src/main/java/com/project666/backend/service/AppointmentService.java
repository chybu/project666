package com.project666.backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project666.backend.domain.CancelAppointmentRequest;
import com.project666.backend.domain.CreateAppointmentRequest;
import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.domain.NoShowAppointmentRequest;

public interface AppointmentService {
    Appointment createAppointment(UUID creatorId, CreateAppointmentRequest request);
    /**
     * A race can happen when creating an appointment. Soft check with service logic and Hard check with db constraint
     * 
     * db constraint checks if the doctor is already scheduled with db constraint
     * CREATE EXTENSION IF NOT EXISTS btree_gist;
     * alter table appointments add constraint no_doctor_overlap exclude using gist (doctor_id with =, tsrange(start_time - interval '30 minutes', end_time + interval '30 minutes') with &&);
     * 
     * check if the patient already has appointment in that timeframe with db constraint
     * alter table appointments add constraint no_patient_overlap exclude using gist (patient_id with =, tsrange(start_time - interval '30 minutes', end_time + interval '30 minutes') with &&);
     */

    Appointment confirmAppointment(UUID receptionistId, UUID appointmentId);

    Page<Appointment> listAppointmentForPatient(UUID patientId, ListAppointmentRequest request, Pageable pageable);

    Page<Appointment> listAppointmentForDoctor(UUID doctorId, ListAppointmentRequest request, Pageable pageable);

    Page<Appointment> listAppointmentForReceptionist(UUID receptionistId, ListAppointmentRequest request, Pageable pageable);

    Page<Appointment> searchAnyAppointmentForReceptionist(UUID receptionistId, ListAppointmentRequest request, Pageable pageable);

    Appointment cancelAppointment(UUID cancellerId, RoleEnum cancellerRole, CancelAppointmentRequest request);
    
    Appointment noShowAppointment(UUID receptionistId, NoShowAppointmentRequest request);
}

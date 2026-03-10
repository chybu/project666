package demo.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import demo.domain.CancelAppointmentRequest;
import demo.domain.CreateAppointmentRequest;
import demo.domain.ListAppointmentRequest;
import demo.domain.entities.Appointment;
import demo.domain.entities.RoleEnum;

public interface AppointmentService {
    Appointment createAppointment(UUID creatorId, CreateAppointmentRequest request);

    Appointment confirmAppointment(UUID receptionistId, UUID appointmentId);

    Page<Appointment> listDoctorAppointment(UUID patientId, ListAppointmentRequest request, Pageable pageable);

    Page<Appointment> listPatientAppointment(UUID doctorId, ListAppointmentRequest request, Pageable pageable);

    Page<Appointment> listConfirmAppointment(UUID receptionistId, ListAppointmentRequest request, Pageable pageable);

    Page<Appointment> listAppointment(ListAppointmentRequest request, Pageable pageable);

    Appointment cancelAppointment(UUID cancellerId, RoleEnum cancellerRole, CancelAppointmentRequest request);
}

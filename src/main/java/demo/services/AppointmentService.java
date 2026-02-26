package demo.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import demo.domains.CancelAppointmentRequest;
import demo.domains.CreateAppointmentRequest;
import demo.domains.ListAppointmentRequest;
import demo.domains.entities.Appointment;

public interface AppointmentService {
    Appointment createAppointment(UUID creatorId, CreateAppointmentRequest request);

    Appointment confirmAppointment(UUID receptionistId, UUID appointmentId);

    Page<Appointment> listDoctorAppointment(UUID patientId, ListAppointmentRequest request, Pageable pageable);

    Page<Appointment> listPatientAppointment(UUID doctorId, ListAppointmentRequest request, Pageable pageable);

    Page<Appointment> listConfirmAppointment(UUID receptionistId, ListAppointmentRequest request, Pageable pageable);

    Page<Appointment> listAppointment(ListAppointmentRequest request, Pageable pageable);

    Appointment cancelAppointment(UUID cancellerId, CancelAppointmentRequest request);
}

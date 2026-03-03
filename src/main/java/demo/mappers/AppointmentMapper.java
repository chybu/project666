package demo.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import demo.domains.CancelAppointmentRequest;
import demo.domains.CreateAppointmentRequest;
import demo.domains.ListAppointmentRequest;
import demo.domains.dtos.CancelAppointmentRequestDto;
import demo.domains.dtos.CancelAppointmentResponseDto;
import demo.domains.dtos.ConfirmAppointmentResponseDto;
import demo.domains.dtos.CreateAppointmentRequestDto;
import demo.domains.dtos.CreateAppointmentResponseDto;
import demo.domains.dtos.ListAppointmentRequestDto;
import demo.domains.dtos.ListAppointmentResponseDto;
import demo.domains.entities.Appointment;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {
    CreateAppointmentRequest fromCreateAppointmentRequesDto(CreateAppointmentRequestDto dto);

    @Mapping(target = "patientFullName", source = "appointment.patient.fullName")
    @Mapping(target = "doctorFullName", source = "appointment.doctor.fullName")
    CreateAppointmentResponseDto toCreateAppointmentResponseDto(Appointment appointment);

    @Mapping(target = "patientFullName", source = "appointment.patient.fullName")
    @Mapping(target = "doctorFullName", source = "appointment.doctor.fullName")
    @Mapping(target = "confirmReceptionistFullName", source = "appointment.confirmReceptionist.fullName")
    ConfirmAppointmentResponseDto toConfirmAppointmentResponseDto(Appointment appointment);

    ListAppointmentRequest fromListAppointmentRequestDto(ListAppointmentRequestDto dto);

    @Mapping(target = "patientFullName", source = "appointment.patient.fullName")
    @Mapping(target = "doctorFullName", source = "appointment.doctor.fullName")
    ListAppointmentResponseDto toListAppointmentResponseDto(Appointment appointment);

    CancelAppointmentRequest fromCancelAppointmentRequestDto(CancelAppointmentRequestDto dto);

    @Mapping(target = "patientFullName", source = "appointment.patient.fullName")
    @Mapping(target = "doctorFullName", source = "appointment.doctor.fullName")
    @Mapping(target = "cancellerFullName", source = "appointment.canceller.fullName")
    CancelAppointmentResponseDto toCancelAppointmentResponseDto(Appointment appointment);
    
}

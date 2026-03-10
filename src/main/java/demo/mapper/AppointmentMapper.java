package demo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import demo.domain.CancelAppointmentRequest;
import demo.domain.CreateAppointmentRequest;
import demo.domain.ListAppointmentRequest;
import demo.domain.dtos.CancelAppointmentRequestDto;
import demo.domain.dtos.CancelAppointmentResponseDto;
import demo.domain.dtos.ConfirmAppointmentResponseDto;
import demo.domain.dtos.CreateAppointmentRequestDto;
import demo.domain.dtos.CreateAppointmentResponseDto;
import demo.domain.dtos.ListAppointmentRequestDto;
import demo.domain.dtos.ListAppointmentResponseDto;
import demo.domain.entities.Appointment;

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

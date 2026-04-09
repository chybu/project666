package com.project666.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.project666.backend.domain.CancelAppointmentRequest;
import com.project666.backend.domain.CreateAppointmentRequest;
import com.project666.backend.domain.ListAppointmentRequest;
import com.project666.backend.domain.dto.CancelAppointmentRequestDto;
import com.project666.backend.domain.dto.CancelAppointmentResponseDto;
import com.project666.backend.domain.dto.ConfirmAppointmentResponseDto;
import com.project666.backend.domain.dto.CreateAppointmentRequestDto;
import com.project666.backend.domain.dto.CreateAppointmentResponseDto;
import com.project666.backend.domain.dto.ListAppointmentRequestDto;
import com.project666.backend.domain.dto.ListAppointmentResponseDto;
import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.NoShowAppointmentRequest;
import com.project666.backend.domain.dto.NoShowAppointmentRequestDto;
import com.project666.backend.domain.dto.NoShowAppointmentResponseDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {
    CreateAppointmentRequest fromCreateAppointmentRequestDto(CreateAppointmentRequestDto dto);

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
    
    NoShowAppointmentRequest fromNoShowAppointmentRequestDto(NoShowAppointmentRequestDto dto);

    @Mapping(target = "patientFullName", source = "appointment.patient.fullName")
    @Mapping(target = "doctorFullName", source = "appointment.doctor.fullName")
    @Mapping(target = "noShowReceptionistFullName", source = "appointment.noShowReceptionist.fullName")
    NoShowAppointmentResponseDto toNoShowAppointmentResponseDto(Appointment appointment);
}

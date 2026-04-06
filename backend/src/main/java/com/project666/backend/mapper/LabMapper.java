package com.project666.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.project666.backend.domain.CreateLabRequestRequest;
import com.project666.backend.domain.ListLabRequestRequest;
import com.project666.backend.domain.ListLabTestRequest;
import com.project666.backend.domain.UpdateLabTestRequest;
import com.project666.backend.domain.dto.CreateLabRequestRequestDto;
import com.project666.backend.domain.dto.CreateLabRequestResponseDto;
import com.project666.backend.domain.dto.ListLabRequestRequestDto;
import com.project666.backend.domain.dto.ListLabRequestResponseDto;
import com.project666.backend.domain.dto.ListLabTestRequestDto;
import com.project666.backend.domain.dto.ListLabTestResponseDto;
import com.project666.backend.domain.dto.PatientLabRequestResponseDto;
import com.project666.backend.domain.dto.UpdateLabTestRequestDto;
import com.project666.backend.domain.entity.LabRequest;
import com.project666.backend.domain.entity.LabTest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LabMapper {

    CreateLabRequestRequest fromCreateLabRequestRequestDto(CreateLabRequestRequestDto dto);

    CreateLabRequestRequest.LabTestRequest fromLabTestRequestDto(CreateLabRequestRequestDto.LabTestRequestDto dto);

    @Mapping(target = "patientFullName", source = "patient.fullName")
    @Mapping(target = "doctorFullName", source = "doctor.fullName")
    @Mapping(target = "appointmentStartTime", source = "appointment.startTime")
    CreateLabRequestResponseDto toCreateLabRequestResponseDto(LabRequest labRequest);

    CreateLabRequestResponseDto.LabTestResponseDto toLabTestResponseDto(LabTest labTest);

    ListLabTestRequest fromListLabTestRequestDto(ListLabTestRequestDto dto);

    @Mapping(target = "patientFullName", source = "labRequest.patient.fullName")
    @Mapping(target = "doctorFullName", source = "labRequest.doctor.fullName")
    @Mapping(target = "labTechnicianFullName", source = "labTechnician.fullName")
    ListLabTestResponseDto toListLabTestResponseDto(LabTest labTest);

    ListLabRequestRequest fromListLabRequestRequestDto(ListLabRequestRequestDto dto);

    @Mapping(target = "patientFullName", source = "patient.fullName")
    @Mapping(target = "doctorFullName", source = "doctor.fullName")
    @Mapping(target = "appointmentStartTime", source = "appointment.startTime")
    PatientLabRequestResponseDto toPatientLabRequestResponseDto(LabRequest labRequest);

    @Mapping(target = "patientFullName", source = "labRequest.patient.fullName")
    @Mapping(target = "doctorFullName", source = "labRequest.doctor.fullName")
    @Mapping(target = "labTechnicianFullName", source = "labTechnician.fullName")
    PatientLabRequestResponseDto.PatientLabTestResponseDto toPatientLabTestResponseDto(LabTest labTest);

    ListLabRequestResponseDto fromPatientLabRequestResponseDto(PatientLabRequestResponseDto dto);

    ListLabTestResponseDto fromPatientLabTestResponseDto(
        PatientLabRequestResponseDto.PatientLabTestResponseDto dto
    );

    @Mapping(target = "patientFullName", source = "patient.fullName")
    @Mapping(target = "doctorFullName", source = "doctor.fullName")
    @Mapping(target = "appointmentStartTime", source = "appointment.startTime")
    ListLabRequestResponseDto toListLabRequestResponseDto(LabRequest request);

    UpdateLabTestRequest fromUpdateLabTestRequestDto(UpdateLabTestRequestDto dto);

}

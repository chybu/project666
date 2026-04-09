package com.project666.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.project666.backend.domain.CreatePrecheckRequest;
import com.project666.backend.domain.ListPrecheckRequest;
import com.project666.backend.domain.dto.CreatePrecheckRequestDto;
import com.project666.backend.domain.dto.ListPrecheckRequestDto;
import com.project666.backend.domain.dto.PrecheckResponseDto;
import com.project666.backend.domain.entity.Precheck;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PrecheckMapper {

    CreatePrecheckRequest fromCreatePrecheckRequestDto(CreatePrecheckRequestDto dto);

    ListPrecheckRequest fromListPrecheckRequestDto(ListPrecheckRequestDto dto);

    @Mapping(target = "appointmentStartTime", source = "appointment.startTime")
    @Mapping(target = "patientFullName", source = "patient.fullName")
    @Mapping(target = "doctorFullName", source = "doctor.fullName")
    @Mapping(target = "nurseFullName", source = "nurse.fullName")
    PrecheckResponseDto toPrecheckResponseDto(Precheck precheck);
}

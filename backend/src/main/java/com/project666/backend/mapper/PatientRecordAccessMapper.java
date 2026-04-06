package com.project666.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.project666.backend.domain.ListPatientRecordAccessRequest;
import com.project666.backend.domain.PatientRecordAccessRequest;
import com.project666.backend.domain.dto.ListPatientRecordAccessRequestDto;
import com.project666.backend.domain.dto.PatientRecordAccessRequestDto;
import com.project666.backend.domain.dto.PatientRecordAccessResponseDto;
import com.project666.backend.domain.entity.PatientRecordAccess;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PatientRecordAccessMapper {

    PatientRecordAccessRequest fromPatientRecordAccessRequestDto(PatientRecordAccessRequestDto dto); 

    @Mapping(target = "patientFullName", source = "patient.fullName")
    @Mapping(target = "doctorFullName", source = "doctor.fullName")
    PatientRecordAccessResponseDto toPatientRecordAccessResponseDto(PatientRecordAccess access);

    ListPatientRecordAccessRequest fromListPatientRecordAccessRequest(ListPatientRecordAccessRequestDto dto);
}
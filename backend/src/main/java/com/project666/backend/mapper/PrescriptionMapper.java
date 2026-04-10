package com.project666.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.project666.backend.domain.CreatePrescriptionMedicineRequest;
import com.project666.backend.domain.CreatePrescriptionRequest;
import com.project666.backend.domain.ListPrescriptionRequest;
import com.project666.backend.domain.dto.CreatePrescriptionMedicineRequestDto;
import com.project666.backend.domain.dto.CreatePrescriptionRequestDto;
import com.project666.backend.domain.dto.ListPrescriptionRequestDto;
import com.project666.backend.domain.dto.PrescriptionMedicineResponseDto;
import com.project666.backend.domain.dto.PrescriptionResponseDto;
import com.project666.backend.domain.entity.Prescription;
import com.project666.backend.domain.entity.PrescriptionMedicine;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PrescriptionMapper {

    CreatePrescriptionRequest fromCreatePrescriptionRequestDto(CreatePrescriptionRequestDto dto);

    CreatePrescriptionMedicineRequest fromCreatePrescriptionMedicineRequestDto(
        CreatePrescriptionMedicineRequestDto dto
    );

    ListPrescriptionRequest fromListPrescriptionRequestDto(ListPrescriptionRequestDto dto);

    PrescriptionMedicineResponseDto toPrescriptionMedicineResponseDto(PrescriptionMedicine medicine);

    @Mapping(target = "doctorFullName", source = "doctor.fullName")
    @Mapping(target = "patientFullName", source = "patient.fullName")
    @Mapping(target = "appointmentStartTime", source = "appointment.startTime")
    PrescriptionResponseDto toPrescriptionResponseDto(Prescription prescription);
}

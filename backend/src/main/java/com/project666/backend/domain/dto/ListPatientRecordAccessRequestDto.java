package com.project666.backend.domain.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.project666.backend.domain.entity.PatientRecordAccessStatusEnum;
import com.project666.backend.domain.entity.PatientRecordTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListPatientRecordAccessRequestDto {
    private UUID patientId;
    private UUID doctorId;
    private PatientRecordTypeEnum type;
    private PatientRecordAccessStatusEnum status;
    private LocalDate createdAtDate;
}

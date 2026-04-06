package com.project666.backend.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project666.backend.domain.entity.PatientRecordAccessStatusEnum;
import com.project666.backend.domain.entity.PatientRecordTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientRecordAccessResponseDto {
    private UUID id;
    private PatientRecordTypeEnum recordType;
    private String patientFullName;
    private String doctorFullName;
    private PatientRecordAccessStatusEnum status;
    private LocalDateTime createdAt;
}

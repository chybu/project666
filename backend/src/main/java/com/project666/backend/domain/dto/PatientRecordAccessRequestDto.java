package com.project666.backend.domain.dto;

import java.util.UUID;

import com.project666.backend.domain.entity.PatientRecordTypeEnum;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientRecordAccessRequestDto {
    @NotNull
    UUID patientId;
    @NotNull
    PatientRecordTypeEnum type;
}

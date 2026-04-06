package com.project666.backend.domain;

import java.util.UUID;

import com.project666.backend.domain.entity.PatientRecordTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientRecordAccessRequest {
    private UUID patientId;
    private PatientRecordTypeEnum type;
}
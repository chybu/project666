package com.project666.backend.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project666.backend.domain.entity.LabTestStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor
public class ListLabTestResponseDto {
    private UUID id;
    private LabTestStatusEnum status;
    private String code;
    private String name;
    private String unit;
    private String result;
    private String labTechnicianNote;
    private String doctorNote;
    private String labTechnicianFullName;
    private String patientFullName;
    private String doctorFullName;
    private LocalDateTime createdAt;
}
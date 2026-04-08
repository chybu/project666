package com.project666.backend.domain.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.project666.backend.domain.entity.LabRequestStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListLabRequestResponseDto {
    private UUID id;
    private LabRequestStatusEnum status;
    private LocalDateTime createdAt;
    private LocalDateTime appointmentStartTime;
    private String patientFullName;
    private String doctorFullName;
    private List<ListLabTestResponseDto> labTests;
}

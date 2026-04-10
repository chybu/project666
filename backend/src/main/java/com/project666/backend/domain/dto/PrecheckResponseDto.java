package com.project666.backend.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project666.backend.domain.entity.PrecheckStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrecheckResponseDto {
    private UUID id;
    private LocalDateTime appointmentStartTime;
    private String patientFullName;
    private String doctorFullName;
    private String nurseFullName;
    private PrecheckStatusEnum status;
    private Integer pulse;
    private Double sugar;
    private Double temperature;
    private Double height;
    private Double weight;
    private String note;
    private LocalDateTime createdAt;
}

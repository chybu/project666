package com.project666.backend.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.project666.backend.domain.entity.LabRequestStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLabRequestResponseDto {
    private String patientFullName;
    private String doctorFullName;
    private LocalDateTime appointmentStartTime;
    private LabRequestStatusEnum status;
    private LocalDateTime createdAt;
    private List<LabTestResponseDto> labTests;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabTestResponseDto {
        private String code;
        private String name;
        private String unit;
        private String doctorNote;
    }
}

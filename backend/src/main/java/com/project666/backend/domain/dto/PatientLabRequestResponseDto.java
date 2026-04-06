package com.project666.backend.domain.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.project666.backend.domain.entity.Appointment;
import com.project666.backend.domain.entity.LabRequestStatusEnum;
import com.project666.backend.domain.entity.LabTestStatusEnum;
import com.project666.backend.domain.entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientLabRequestResponseDto {
    private UUID id;
    private LabRequestStatusEnum status;
    private LocalDateTime createdAt;
    private LocalDateTime appointmentStartTime;
    private String patientFullName;
    private String doctorFullName;
    private List<PatientLabTestResponseDto> labTests;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientLabTestResponseDto {
        // dont have doctor and lab tech note
        private UUID id;
        private String code;
        private String name;
        private String unit;
        private LabTestStatusEnum status;
        private String result;
        private String labTechnicianFullName;
        private String patientFullName;
        private String doctorFullName;
        private LocalDateTime createdAt;
    }
}

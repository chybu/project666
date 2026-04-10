package com.project666.backend.domain.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.project666.backend.domain.entity.PrescriptionStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponseDto {
    private java.util.UUID id;
    private String doctorFullName;
    private String patientFullName;
    private LocalDateTime appointmentStartTime;
    private PrescriptionStatusEnum status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalRefills;
    private Integer remainingRefills;
    private Integer refillIntervalDays;
    private LocalDateTime nextEligibleRefillAt;
    private LocalDateTime lastConsumedAt;
    private String generalNote;
    private LocalDateTime createdAt;
    private List<PrescriptionMedicineResponseDto> medicines = new ArrayList<>();
}

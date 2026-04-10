package com.project666.backend.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrescriptionRequest {
    private UUID appointmentId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalRefills;
    private Integer refillIntervalDays;
    private String generalNote;
    private List<CreatePrescriptionMedicineRequest> medicines = new ArrayList<>();
}

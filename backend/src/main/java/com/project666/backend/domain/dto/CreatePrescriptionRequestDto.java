package com.project666.backend.domain.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrescriptionRequestDto {
    @NotNull
    private UUID appointmentId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    @Min(0)
    private Integer totalRefills;

    @NotNull
    @Min(0)
    private Integer refillIntervalDays;

    private String generalNote;

    @NotEmpty
    @Valid
    private List<CreatePrescriptionMedicineRequestDto> medicines = new ArrayList<>();
}

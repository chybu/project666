package com.project666.backend.domain.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrecheckRequestDto {
    @NotNull
    private UUID appointmentId;

    @NotNull
    @Positive
    private Integer pulse;

    @NotNull
    @Positive
    private Double sugar;

    @NotNull
    @Positive
    private Double temperature;

    @NotNull
    @Positive
    private Double height;

    @NotNull
    @Positive
    private Double weight;

    private String note;
}

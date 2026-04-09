package com.project666.backend.domain.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoShowAppointmentRequestDto {
    @NotNull
    private UUID appointmentId;
}
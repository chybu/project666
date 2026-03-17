package com.project666.backend.domain.dto;

import java.util.UUID;

import com.project666.backend.domain.entity.CancellationInitiatorEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelAppointmentRequestDto {
    @NotNull
    private UUID appointmentId;
    @NotBlank
    private String cancelReason;
    @NotNull
    private CancellationInitiatorEnum cancellationInitiator;
}

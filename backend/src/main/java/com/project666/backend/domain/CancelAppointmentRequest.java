package com.project666.backend.domain;

import java.util.UUID;

import com.project666.backend.domain.entity.CancellationInitiatorEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelAppointmentRequest {
    private UUID appointmentId;
    private String cancelReason;
    private CancellationInitiatorEnum cancellationInitiator;
}

package demo.domain;

import java.util.UUID;

import demo.domain.entities.CancellationInitiatorEnum;
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

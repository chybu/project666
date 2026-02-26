package demo.domains.dtos;

import java.util.UUID;

import demo.domains.entities.CancellationInitiatorEnum;
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

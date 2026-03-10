package demo.domain.dtos;

import java.time.LocalDateTime;

import demo.domain.entities.AppointmentStatusEnum;
import demo.domain.entities.AppointmentTypeEnum;
import demo.domain.entities.CancellationInitiatorEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelAppointmentResponseDto {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentTypeEnum type;
    private AppointmentStatusEnum status;
    private String patientFullName;
    private String doctorFullName;
    private String cancellerFullName;
    private CancellationInitiatorEnum cancellationInitiator;
    private String cancelReason;
    private LocalDateTime cancelledAt;
}

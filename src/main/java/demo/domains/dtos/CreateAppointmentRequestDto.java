package demo.domains.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import demo.domains.entities.AppointmentTypeEnum;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequestDto {
    @NotNull
    @Future
    private LocalDateTime startTime;
    @NotNull
    private AppointmentTypeEnum type;
    @NotNull
    private UUID doctorId;
    @NotNull
    private UUID patientId;
}

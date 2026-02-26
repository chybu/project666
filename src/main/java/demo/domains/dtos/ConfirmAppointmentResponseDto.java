package demo.domains.dtos;

import java.time.LocalDateTime;

import demo.domains.entities.AppointmentStatusEnum;
import demo.domains.entities.AppointmentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmAppointmentResponseDto {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentTypeEnum type;
    private AppointmentStatusEnum status;
    private String patientFullName;
    private String doctorFullName;
    private String confirmReceptionistFullName;
    private LocalDateTime confirmedAt;
}

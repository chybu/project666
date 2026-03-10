package demo.domain.dtos;

import java.time.LocalDate;
import java.util.UUID;

import demo.domain.entities.AppointmentStatusEnum;
import demo.domain.entities.AppointmentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListAppointmentRequestDto {
    private UUID patientId;
    private UUID doctorId;
    private AppointmentTypeEnum type;
    private AppointmentStatusEnum status;
    private LocalDate from;
    private LocalDate end;
}

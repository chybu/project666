package com.project666.backend.domain.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.domain.entity.AppointmentTypeEnum;
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

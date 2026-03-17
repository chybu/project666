package com.project666.backend.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project666.backend.domain.entity.AppointmentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {
    private LocalDateTime startTime;
    private AppointmentTypeEnum type;
    private UUID doctorId;
    private UUID patientId;
}

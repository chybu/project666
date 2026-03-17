package com.project666.backend.domain.dto;

import java.time.LocalDateTime;

import com.project666.backend.domain.entity.AppointmentStatusEnum;
import com.project666.backend.domain.entity.AppointmentTypeEnum;
import com.project666.backend.domain.entity.CancellationInitiatorEnum;
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

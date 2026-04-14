package com.project666.backend.domain;

import java.time.LocalDate;
import java.util.UUID;

import com.project666.backend.domain.entity.LabRequestStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListLabRequestRequest {
    private LabRequestStatusEnum status;
    private LocalDate minDate;
    private LocalDate maxDate;
    private LocalDate createdAtDate;
    private UUID doctorId;
    private UUID patientId;
    private UUID appointmentId;
}

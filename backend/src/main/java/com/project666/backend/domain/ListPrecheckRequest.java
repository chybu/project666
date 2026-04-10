package com.project666.backend.domain;

import java.time.LocalDate;
import java.util.UUID;

import com.project666.backend.domain.entity.PrecheckStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListPrecheckRequest {
    private UUID appointmentId;
    private UUID patientId;
    private UUID doctorId;
    private UUID nurseId;
    private PrecheckStatusEnum status;
    private LocalDate createdAtDate;
}

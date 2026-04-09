package com.project666.backend.domain.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.project666.backend.domain.entity.PrescriptionStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListPrescriptionRequestDto {
    private UUID patientId;
    private UUID doctorId;
    private UUID appointmentId;
    private PrescriptionStatusEnum status;
    private String medicineName;
    private LocalDate createdAtDate;
}

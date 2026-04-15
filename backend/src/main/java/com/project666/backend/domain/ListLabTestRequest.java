package com.project666.backend.domain;

import java.time.LocalDate;
import java.util.UUID;

import com.project666.backend.domain.entity.LabTestStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListLabTestRequest {
    private UUID doctorId;
    private UUID patientId;
    private LabTestStatusEnum status;
    private String code;
    private String name;
    private String unit;
    private LocalDate minDate;
    private LocalDate maxDate;
}

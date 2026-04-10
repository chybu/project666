package com.project666.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrescriptionMedicineRequest {
    private String medicineName;
    private String dosage;
    private String frequency;
    private String route;
    private String instructions;
    private String quantity;
}

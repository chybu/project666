package com.project666.backend.domain;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLabTestRequest {
    UUID labTestId;
    private String code;
    private String name;
    private String unit;
    private String result;
    private String labTechnicianNote;
}
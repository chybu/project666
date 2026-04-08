package com.project666.backend.domain.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateLabTestRequestDto {
    UUID labTestId;
    private String code;
    private String name;
    private String unit;
    private String result;
    private String labTechnicianNote;
}

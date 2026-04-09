package com.project666.backend.domain;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrecheckRequest {
    private UUID appointmentId;
    private Integer pulse;
    private Double sugar;
    private Double temperature;
    private Double height;
    private Double weight;
    private String note;
}

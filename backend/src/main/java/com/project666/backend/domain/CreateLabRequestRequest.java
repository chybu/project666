package com.project666.backend.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateLabRequestRequest {
    private UUID appointmentId;
    private List<LabTestRequest> labTests = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabTestRequest {
        private String code;
        private String name;
        private String unit;
        private String doctorNote;
    }
}
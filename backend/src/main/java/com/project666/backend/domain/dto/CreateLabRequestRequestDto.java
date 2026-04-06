package com.project666.backend.domain.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLabRequestRequestDto {
    @NotNull
    private UUID appointmentId;

    @NotEmpty
    @Valid
    private List<LabTestRequestDto> labTests = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabTestRequestDto {
        private String code;

        @NotBlank
        private String name;

        private String unit;

        private String doctorNote;
    }
}
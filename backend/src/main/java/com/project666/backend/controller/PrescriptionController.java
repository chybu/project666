package com.project666.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project666.backend.domain.CreatePrescriptionRequest;
import com.project666.backend.domain.ListPrescriptionRequest;
import com.project666.backend.domain.dto.CreatePrescriptionRequestDto;
import com.project666.backend.domain.dto.ListPrescriptionRequestDto;
import com.project666.backend.domain.dto.PrescriptionResponseDto;
import com.project666.backend.domain.entity.Prescription;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.mapper.PrescriptionMapper;
import com.project666.backend.service.PrescriptionService;
import com.project666.backend.util.JwtUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/v1/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final PrescriptionMapper prescriptionMapper;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponseDto> createPrescription(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody @Valid CreatePrescriptionRequestDto requestDto
    ) {
        CreatePrescriptionRequest request = prescriptionMapper.fromCreatePrescriptionRequestDto(requestDto);
        Prescription prescription = prescriptionService.createPrescription(JwtUtil.getUserId(jwt), request);
        return new ResponseEntity<>(
            prescriptionMapper.toPrescriptionResponseDto(prescription),
            HttpStatus.CREATED
        );
    }

    @PostMapping("/list")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    public ResponseEntity<Page<PrescriptionResponseDto>> listPrescriptions(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody ListPrescriptionRequestDto requestDto
    ) {
        ListPrescriptionRequest request = prescriptionMapper.fromListPrescriptionRequestDto(requestDto);
        java.util.UUID requesterId = JwtUtil.getUserId(jwt);
        RoleEnum role = JwtUtil.getRole(jwt);
        Page<PrescriptionResponseDto> response;

        switch (role) {
            case PATIENT -> {
                request.setPatientId(requesterId);
                response = prescriptionService
                    .listPrescriptionForPatient(requesterId, request, pageable)
                    .map(prescriptionMapper::toPrescriptionResponseDto);
            }
            case DOCTOR -> {
                request.setDoctorId(requesterId);
                response = prescriptionService
                    .listPrescriptionForDoctor(requesterId, request, pageable)
                    .map(prescriptionMapper::toPrescriptionResponseDto);
            }
            default -> throw new IllegalArgumentException(String.format("%s role is not known", role.name()));
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/shared/list")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Page<PrescriptionResponseDto>> listSharedDoctorPrescriptions(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody ListPrescriptionRequestDto requestDto
    ) {
        ListPrescriptionRequest request = prescriptionMapper.fromListPrescriptionRequestDto(requestDto);
        Page<PrescriptionResponseDto> response = prescriptionService
            .listPrescriptionForNewDoctor(JwtUtil.getUserId(jwt), request, pageable)
            .map(prescriptionMapper::toPrescriptionResponseDto);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{prescriptionId}/cancel")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponseDto> cancelPrescription(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable java.util.UUID prescriptionId
    ) {
        Prescription prescription = prescriptionService.cancelPrescription(JwtUtil.getUserId(jwt), prescriptionId);
        return ResponseEntity.ok(prescriptionMapper.toPrescriptionResponseDto(prescription));
    }

    @PutMapping("/{prescriptionId}/consume-refill")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PrescriptionResponseDto> consumeRefill(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable java.util.UUID prescriptionId
    ) {
        Prescription prescription = prescriptionService.consumeRefill(JwtUtil.getUserId(jwt), prescriptionId);
        return ResponseEntity.ok(prescriptionMapper.toPrescriptionResponseDto(prescription));
    }
}

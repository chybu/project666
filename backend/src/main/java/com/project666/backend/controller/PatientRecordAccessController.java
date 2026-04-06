package com.project666.backend.controller;

import java.util.UUID;

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

import com.project666.backend.domain.ListPatientRecordAccessRequest;
import com.project666.backend.domain.PatientRecordAccessRequest;
import com.project666.backend.domain.dto.ListPatientRecordAccessRequestDto;
import com.project666.backend.domain.dto.PatientRecordAccessRequestDto;
import com.project666.backend.domain.dto.PatientRecordAccessResponseDto;
import com.project666.backend.domain.entity.PatientRecordAccess;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.mapper.PatientRecordAccessMapper;
import com.project666.backend.service.PatientRecordAccessService;
import com.project666.backend.util.JwtUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/v1/patient-record-access")
@RequiredArgsConstructor
public class PatientRecordAccessController {
    private final PatientRecordAccessService patientRecordAccessService;
    private final PatientRecordAccessMapper patientRecordAccessMapper;

    @PostMapping("/request")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PatientRecordAccessResponseDto> createAccessRequest(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody @Valid PatientRecordAccessRequestDto requestDto
    ){
        PatientRecordAccessRequest request = patientRecordAccessMapper.fromPatientRecordAccessRequestDto(requestDto);
        PatientRecordAccess access = patientRecordAccessService.requestPatientRecordAccess(JwtUtil.getUserId(jwt), request);
        PatientRecordAccessResponseDto responseDto = patientRecordAccessMapper.toPatientRecordAccessResponseDto(access);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/list")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    public ResponseEntity<Page<PatientRecordAccessResponseDto>> listPatientRecordAccess(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody @Valid ListPatientRecordAccessRequestDto requestDto
    ) {
        ListPatientRecordAccessRequest request =
            patientRecordAccessMapper.fromListPatientRecordAccessRequest(requestDto);

        UUID requesterId = JwtUtil.getUserId(jwt);
        RoleEnum role = JwtUtil.getRole(jwt);

        Page<PatientRecordAccess> accesses;

        switch (role) {
            case PATIENT -> accesses = patientRecordAccessService
                .listPatientRecordAccess(requesterId, request, pageable);

            case DOCTOR -> accesses = patientRecordAccessService
                .listSharedPatientRecordAccess(requesterId, request, pageable);

            default -> throw new IllegalArgumentException(
                String.format("%s role is not allowed", role.name())
            );
        }

        return ResponseEntity.ok(
            accesses.map(patientRecordAccessMapper::toPatientRecordAccessResponseDto)
        );
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PatientRecordAccessResponseDto> cancel(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id
    ) {
        UUID doctorId = JwtUtil.getUserId(jwt);
        PatientRecordAccess access = patientRecordAccessService.cancel(doctorId, id);
        return ResponseEntity.ok(patientRecordAccessMapper.toPatientRecordAccessResponseDto(access));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientRecordAccessResponseDto> approve(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id
    ) {
        UUID patientId = JwtUtil.getUserId(jwt);
        PatientRecordAccess access = patientRecordAccessService.approve(patientId, id);
        return ResponseEntity.ok(patientRecordAccessMapper.toPatientRecordAccessResponseDto(access));
    }

    @PutMapping("/{id}/deny")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientRecordAccessResponseDto> deny(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id
    ) {
        UUID patientId = JwtUtil.getUserId(jwt);
        PatientRecordAccess access = patientRecordAccessService.deny(patientId, id);
        return ResponseEntity.ok(patientRecordAccessMapper.toPatientRecordAccessResponseDto(access));
    }

    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientRecordAccessResponseDto> revoke(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id
    ) {
        UUID patientId = JwtUtil.getUserId(jwt);
        PatientRecordAccess access = patientRecordAccessService.revoke(patientId, id);
        return ResponseEntity.ok(patientRecordAccessMapper.toPatientRecordAccessResponseDto(access));
    }
}
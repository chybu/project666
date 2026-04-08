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

import com.project666.backend.domain.CreateLabRequestRequest;
import com.project666.backend.domain.ListLabRequestRequest;
import com.project666.backend.domain.ListLabTestRequest;
import com.project666.backend.domain.UpdateLabTestRequest;
import com.project666.backend.domain.dto.CreateLabRequestRequestDto;
import com.project666.backend.domain.dto.CreateLabRequestResponseDto;
import com.project666.backend.domain.dto.ListLabRequestRequestDto;
import com.project666.backend.domain.dto.ListLabRequestResponseDto;
import com.project666.backend.domain.dto.ListLabTestResponseDto;
import com.project666.backend.domain.dto.UpdateLabTestRequestDto;
import com.project666.backend.domain.dto.ListLabTestRequestDto;
import com.project666.backend.domain.entity.LabRequest;
import com.project666.backend.domain.entity.LabTest;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.mapper.LabMapper;
import com.project666.backend.service.LabService;
import com.project666.backend.util.JwtUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/v1/labs")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;
    private final LabMapper labMapper;

    @PostMapping("/requests/create")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<CreateLabRequestResponseDto> createLabRequest(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody @Valid CreateLabRequestRequestDto requestDto
    ){
        CreateLabRequestRequest request = labMapper.fromCreateLabRequestRequestDto(requestDto);
        LabRequest labRequest = labService.createLabRequest(JwtUtil.getUserId(jwt), request);
        CreateLabRequestResponseDto responseDto = labMapper.toCreateLabRequestResponseDto(labRequest);

        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/requests/list")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'LAB_TECHNICIAN')")
    public ResponseEntity<Page<ListLabRequestResponseDto>> listLabRequests(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody ListLabRequestRequestDto requestDto
    ) {
        ListLabRequestRequest request = labMapper.fromListLabRequestRequestDto(requestDto);
        UUID requesterId = JwtUtil.getUserId(jwt);
        RoleEnum role = JwtUtil.getRole(jwt);

        Page<ListLabRequestResponseDto> labRequests;

        switch (role) {
            case PATIENT -> labRequests = labService
                .listLabRequestForPatient(requesterId, request, pageable)
                .map(labMapper::fromPatientLabRequestResponseDto);

            case DOCTOR -> labRequests = labService
                .listLabRequestForDoctor(requesterId, request, pageable)
                .map(labMapper::toListLabRequestResponseDto);

            case LAB_TECHNICIAN -> labRequests = labService
                .listLabRequestForLabTechnician(requesterId, request, pageable)
                .map(labMapper::toListLabRequestResponseDto);

            default -> throw new IllegalArgumentException(String.format("%s role is not known", role.name()));
        }

        return ResponseEntity.ok(labRequests);
    }

    @PostMapping("/requests/shared/list")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Page<ListLabRequestResponseDto>> listSharedLabRequestsForDoctor(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody @Valid ListLabRequestRequestDto requestDto
    ) {
        ListLabRequestRequest request = labMapper.fromListLabRequestRequestDto(requestDto);
        UUID doctorId = JwtUtil.getUserId(jwt);

        Page<ListLabRequestResponseDto> labRequests = labService
            .listLabRequestForNewDoctor(doctorId, request, pageable)
            .map(labMapper::toListLabRequestResponseDto);

        return ResponseEntity.ok(labRequests);
    }


    @PostMapping("/tests/list")
    @PreAuthorize("hasRole('LAB_TECHNICIAN')")
    public ResponseEntity<Page<ListLabTestResponseDto>> listLabTestForStaff(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody @Valid ListLabTestRequestDto requestDto
    ){
        ListLabTestRequest request = labMapper.fromListLabTestRequestDto(requestDto);
        Page<LabTest> labTests = labService.listLabTestForLabTechnician(JwtUtil.getUserId(jwt), request, pageable);

        return ResponseEntity.ok(labTests.map(labMapper::toListLabTestResponseDto));
    }

    @PutMapping("/requests/{requestId}/cancel")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<CreateLabRequestResponseDto> cancelLabRequest(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID requestId
    ){
        LabRequest labRequest = labService.cancelLabRequest(JwtUtil.getUserId(jwt), requestId);
        CreateLabRequestResponseDto responseDto = labMapper.toCreateLabRequestResponseDto(labRequest);
        
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @PutMapping("/tests/{labTestId}/claim")
    @PreAuthorize("hasRole('LAB_TECHNICIAN')")
    public ResponseEntity<ListLabTestResponseDto> claimLabTest(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID labTestId
    ) {
        LabTest labTest = labService.claimLabTest(JwtUtil.getUserId(jwt), labTestId);
        return ResponseEntity.ok(labMapper.toListLabTestResponseDto(labTest));
    }

    @PutMapping("/tests/update")
    @PreAuthorize("hasRole('LAB_TECHNICIAN')")
    public ResponseEntity<ListLabTestResponseDto> updateLabTest(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody @Valid UpdateLabTestRequestDto requestDto
    ) {
        UpdateLabTestRequest request = labMapper.fromUpdateLabTestRequestDto(requestDto);
        LabTest labTest = labService.updateLabTest(JwtUtil.getUserId(jwt), request);
        return ResponseEntity.ok(labMapper.toListLabTestResponseDto(labTest));
    }

    @PutMapping("/tests/{labTestId}/submit")
    @PreAuthorize("hasRole('LAB_TECHNICIAN')")
    public ResponseEntity<ListLabTestResponseDto> submitLabTest(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID labTestId
    ) {
        LabTest labTest = labService.submitLabTest(JwtUtil.getUserId(jwt), labTestId);
        return ResponseEntity.ok(labMapper.toListLabTestResponseDto(labTest));
    }
}

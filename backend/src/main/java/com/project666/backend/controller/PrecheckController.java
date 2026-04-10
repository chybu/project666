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

import com.project666.backend.domain.CreatePrecheckRequest;
import com.project666.backend.domain.ListPrecheckRequest;
import com.project666.backend.domain.dto.CreatePrecheckRequestDto;
import com.project666.backend.domain.dto.ListPrecheckRequestDto;
import com.project666.backend.domain.dto.PrecheckResponseDto;
import com.project666.backend.domain.entity.Precheck;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.mapper.PrecheckMapper;
import com.project666.backend.service.PrecheckService;
import com.project666.backend.util.JwtUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/v1/prechecks")
@RequiredArgsConstructor
public class PrecheckController {

    private final PrecheckService precheckService;
    private final PrecheckMapper precheckMapper;

    @PostMapping("/create")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<PrecheckResponseDto> createPrecheck(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody @Valid CreatePrecheckRequestDto requestDto
    ) {
        CreatePrecheckRequest request = precheckMapper.fromCreatePrecheckRequestDto(requestDto);
        Precheck precheck = precheckService.createPrecheck(JwtUtil.getUserId(jwt), request);
        return new ResponseEntity<>(precheckMapper.toPrecheckResponseDto(precheck), HttpStatus.CREATED);
    }

    @PutMapping("/{precheckId}/cancel")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<PrecheckResponseDto> cancelPrecheck(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID precheckId
    ) {
        Precheck precheck = precheckService.cancelPrecheck(JwtUtil.getUserId(jwt), precheckId);
        return ResponseEntity.ok(precheckMapper.toPrecheckResponseDto(precheck));
    }

    @PostMapping("/list")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'NURSE')")
    public ResponseEntity<Page<PrecheckResponseDto>> listPrechecks(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody @Valid ListPrecheckRequestDto requestDto
    ) {
        ListPrecheckRequest request = precheckMapper.fromListPrecheckRequestDto(requestDto);
        UUID requesterId = JwtUtil.getUserId(jwt);
        RoleEnum role = JwtUtil.getRole(jwt);

        Page<Precheck> prechecks;

        switch (role) {
            case PATIENT -> prechecks = precheckService.listPrecheckForPatient(requesterId, request, pageable);
            case DOCTOR -> prechecks = precheckService.listPrecheckForDoctor(requesterId, request, pageable);
            case NURSE -> prechecks = precheckService.listPrecheckForNurse(requesterId, request, pageable);
            default -> throw new IllegalArgumentException(String.format("%s role is not allowed", role.name()));
        }

        return ResponseEntity.ok(prechecks.map(precheckMapper::toPrecheckResponseDto));
    }

    @PostMapping("/shared/list")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Page<PrecheckResponseDto>> listSharedPrechecks(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody @Valid ListPrecheckRequestDto requestDto
    ) {
        ListPrecheckRequest request = precheckMapper.fromListPrecheckRequestDto(requestDto);
        Page<Precheck> prechecks = precheckService.listSharedPrecheckForDoctor(
            JwtUtil.getUserId(jwt),
            request,
            pageable
        );

        return ResponseEntity.ok(prechecks.map(precheckMapper::toPrecheckResponseDto));
    }
}

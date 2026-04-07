package com.project666.backend.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import com.project666.backend.domain.ListAppointmentBillRequest;
import com.project666.backend.domain.ListLabBillRequest;
import com.project666.backend.domain.dto.ListAppointmentBillRequestDto;
import com.project666.backend.domain.dto.ListBillResponseDto;
import com.project666.backend.domain.dto.ListLabBillRequestDto;
import com.project666.backend.domain.entity.AppointmentBill;
import com.project666.backend.domain.entity.BaseBill;
import com.project666.backend.domain.entity.LabBill;
import com.project666.backend.domain.entity.RoleEnum;
import com.project666.backend.mapper.BillMapper;
import com.project666.backend.service.BillService;
import com.project666.backend.util.JwtUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/v1/bills")
@RequiredArgsConstructor
public class BillController {
    private final BillService billService;
    private final BillMapper billMapper;

    @PostMapping("/appointments/list")
    @PreAuthorize("hasAnyRole('PATIENT', 'ACCOUNTANT')")
    public ResponseEntity<Page<ListBillResponseDto>> listAppointmentBills(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody @Valid ListAppointmentBillRequestDto requestDto
    ) {
        ListAppointmentBillRequest request = billMapper.fromListAppointmentBillRequestDto(requestDto);
        UUID requesterId = JwtUtil.getUserId(jwt);
        RoleEnum role = JwtUtil.getRole(jwt);

        Page<AppointmentBill> bills;

        switch (role) {
            case PATIENT -> bills = billService.listAppointmentBillForPatient(
                requesterId,
                request,
                pageable
            );
            case ACCOUNTANT -> bills = billService.listAppointmentBillForAccountant(
                requesterId,
                request,
                pageable
            );
            default -> throw new IllegalArgumentException(
                String.format("%s role is not known", role.name())
            );
        }

        return ResponseEntity.ok(
            bills.map(billMapper::toListBillResponseDto)
        );
    }

    @PostMapping("/appointments/search")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public ResponseEntity<Page<ListBillResponseDto>> searchAnyAppointmentBills(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody @Valid ListAppointmentBillRequestDto requestDto
    ) {
        ListAppointmentBillRequest request = billMapper.fromListAppointmentBillRequestDto(requestDto);

        Page<AppointmentBill> bills = billService.searchAnyAppointmentBillForAccountant(
            JwtUtil.getUserId(jwt),
            request,
            pageable
        );

        return ResponseEntity.ok(
            bills.map(billMapper::toListBillResponseDto)
        );
    }

    @PostMapping("/labs/list")
    @PreAuthorize("hasAnyRole('PATIENT', 'ACCOUNTANT')")
    public ResponseEntity<Page<ListBillResponseDto>> listLabBills(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody @Valid ListLabBillRequestDto requestDto
    ) {
        ListLabBillRequest request = billMapper.fromListLabBillRequestDto(requestDto);
        UUID requesterId = JwtUtil.getUserId(jwt);
        RoleEnum role = JwtUtil.getRole(jwt);

        Page<LabBill> bills;

        switch (role) {
            case PATIENT -> bills = billService.listLabBillForPatient(
                requesterId,
                request,
                pageable
            );
            case ACCOUNTANT -> bills = billService.listLabBillForAccountant(
                requesterId,
                request,
                pageable
            );
            default -> throw new IllegalArgumentException(
                String.format("%s role is not known", role.name())
            );
        }

        return ResponseEntity.ok(
            bills.map(billMapper::toListBillResponseDto)
        );
    }

    @PostMapping("labs/search")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public ResponseEntity<Page<ListBillResponseDto>> searchAnyLabBills(
        @AuthenticationPrincipal Jwt jwt,
        Pageable pageable,
        @RequestBody @Valid ListLabBillRequestDto requestDto
    ) {
        ListLabBillRequest request = billMapper.fromListLabBillRequestDto(requestDto);

        Page<LabBill> bills = billService.searchAnyLabBillForAccountant(
            JwtUtil.getUserId(jwt),
            request,
            pageable
        );

        return ResponseEntity.ok(
            bills.map(billMapper::toListBillResponseDto)
        );
    }

    @PutMapping("/{billId}/confirm-payment")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public ResponseEntity<ListBillResponseDto> confirmBillPayment(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID billId
    ) {
        BaseBill bill = billService.confirmBillPayment(
            JwtUtil.getUserId(jwt),
            billId
        );

        return ResponseEntity.ok(
            billMapper.toListBillResponseDto(bill)
        );
    }
}

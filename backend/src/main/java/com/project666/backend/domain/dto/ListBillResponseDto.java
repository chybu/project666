package com.project666.backend.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.project666.backend.domain.entity.BillStatusEnum;
import com.project666.backend.domain.entity.BillTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListBillResponseDto {
    UUID id;
    private BigDecimal amount;
    private BigDecimal insuranceCoverAmount;
    private BigDecimal patientPaymentAmount;
    private BillStatusEnum status;
    private BillTypeEnum type;
    private String patientFullName;
    private String confirmAccountantFullName;
    private LocalDateTime paidOn;
    private LocalDateTime createdAt;
}
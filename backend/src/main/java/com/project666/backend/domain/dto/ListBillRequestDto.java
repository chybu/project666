package com.project666.backend.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.project666.backend.domain.entity.BillStatusEnum;
import com.project666.backend.domain.entity.BillTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListBillRequestDto {
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal minInsuranceCoverAmount;
    private BigDecimal maxInsuranceCoverAmount;
    private BigDecimal minPatientPaymentAmount;
    private BigDecimal maxPatientPaymentAmount;
    private BillStatusEnum status;
    private BillTypeEnum type;
    private UUID patientId;
    private UUID confirmAccountantId;
    private LocalDate from;
    private LocalDate end;
    private LocalDate paidOnDate;
    private LocalDate createdAtDate;
}

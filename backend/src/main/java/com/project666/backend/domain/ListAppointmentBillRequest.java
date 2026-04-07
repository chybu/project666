package com.project666.backend.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.project666.backend.domain.entity.BillStatusEnum;
import com.project666.backend.domain.entity.BillTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListAppointmentBillRequest {
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal minInsuranceCoverAmount;
    private BigDecimal maxInsuranceCoverAmount;
    private BigDecimal minPatientPaymentAmount;
    private BigDecimal maxPatientPaymentAmount;
    private BillStatusEnum status;
    private BillTypeEnum type;
    private UUID appointmentId;
    private LocalDate paidOnDate;
    private LocalDate createdAtDate;    
}
package com.project666.backend.domain.message;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceCompletedMessage {
    private UUID billId;
    private BigDecimal insuranceCoverAmount;
    private BigDecimal patientPaymentAmount;
}

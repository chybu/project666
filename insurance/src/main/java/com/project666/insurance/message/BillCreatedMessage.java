package com.project666.insurance.message;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillCreatedMessage {
    private UUID billId;
    private BigDecimal amount;
    private String billType;
    private UUID patientId;
}
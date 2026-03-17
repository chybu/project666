package com.project666.backend.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseBill {

    /**
     * TODO:
     * 1. two kind of bill: appointmentBill and labBill
     * 2. add bill to confirm (service bill and late bill)
     * 3. add bill to cancel
     */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "amount", updatable = false, nullable = false)
    private BigDecimal amount;

    @Column(name = "insurance_cover_amount")
    private BigDecimal insuranceCoverAmount;

    @Column(name = "patient_payment_amount")
    private BigDecimal patientPaymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BillStatusEnum status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BillTypeEnum type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", updatable = false, nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirm_accountant_id")
    private User confirmAccountant;

    @Column(name = "paid_on")
    private LocalDateTime paidOn;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}


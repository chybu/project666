package com.project666.backend.specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.project666.backend.domain.entity.BaseBill;
import com.project666.backend.domain.entity.BillStatusEnum;
import com.project666.backend.domain.entity.BillTypeEnum;

public final class BaseBillSpecification {

    private BaseBillSpecification() {
    }

    public static <T extends BaseBill> Specification<T> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static <T extends BaseBill> Specification<T> byPatient(UUID patientId) {
        return (root, query, cb) ->
            patientId == null ? null :
            cb.equal(root.get("patient").get("id"), patientId);
    }

    public static <T extends BaseBill> Specification<T> byConfirmAccountant(UUID accountantId) {
        return (root, query, cb) ->
            accountantId == null ? null :
            cb.equal(root.get("confirmAccountant").get("id"), accountantId);
    }

    public static <T extends BaseBill> Specification<T> byStatus(BillStatusEnum status) {
        return (root, query, cb) ->
            status == null ? null :
            cb.equal(root.get("status"), status);
    }

    public static <T extends BaseBill> Specification<T> byType(BillTypeEnum type) {
        return (root, query, cb) ->
            type == null ? null :
            cb.equal(root.get("type"), type);
    }

    public static <T extends BaseBill> Specification<T> byAmountRange(
        BigDecimal min,
        BigDecimal max
    ) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("amount"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("amount"), min);
            return cb.lessThanOrEqualTo(root.get("amount"), max);
        };
    }

    public static <T extends BaseBill> Specification<T> byInsuranceCoverAmountRange(
        BigDecimal min,
        BigDecimal max
    ) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("insuranceCoverAmount"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("insuranceCoverAmount"), min);
            return cb.lessThanOrEqualTo(root.get("insuranceCoverAmount"), max);
        };
    }

    public static <T extends BaseBill> Specification<T> byPatientPaymentAmountRange(
        BigDecimal min,
        BigDecimal max
    ) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("patientPaymentAmount"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("patientPaymentAmount"), min);
            return cb.lessThanOrEqualTo(root.get("patientPaymentAmount"), max);
        };
    }

    public static <T extends BaseBill> Specification<T> byPaidOnDate(LocalDate paidOnDate) {
        return (root, query, cb) -> {
            if (paidOnDate == null) return null;
            return cb.between(
                root.get("paidOn"),
                paidOnDate.atStartOfDay(),
                paidOnDate.atTime(LocalTime.MAX)
            );
        };
    }

    public static <T extends BaseBill> Specification<T> byCreatedAtDate(LocalDate createdAtDate) {
        return (root, query, cb) -> {
            if (createdAtDate == null) return null;
            return cb.between(
                root.get("createdAt"),
                createdAtDate.atStartOfDay(),
                createdAtDate.atTime(LocalTime.MAX)
            );
        };
    }
}

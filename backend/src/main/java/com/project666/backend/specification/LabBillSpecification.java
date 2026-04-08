package com.project666.backend.specification;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.project666.backend.domain.entity.LabBill;

public final class LabBillSpecification {
    private LabBillSpecification() {
    }

    public static Specification<LabBill> byLabTest(UUID labTestId) {
        return (root, query, cb) ->
            labTestId == null ? null :
            cb.equal(root.get("labTest").get("id"), labTestId);
    }
}

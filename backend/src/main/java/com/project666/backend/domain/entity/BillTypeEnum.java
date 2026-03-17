package com.project666.backend.domain.entity;

public enum BillTypeEnum {
    // base fee
    QUICK_CHECK_FEE,
    MID_CHECK_FEE,
    LONG_CHECK_FEE,

    // cancellation fee
    QUICK_CHECK_CANCELLATION_FEE,
    MID_CHECK_CANCELLATION_FEE,
    LONG_CHECK_CANCELLATION_FEE,

    // late fee
    LATE_FEE
}

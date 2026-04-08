package com.project666.backend.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.project666.backend.domain.entity.BaseBill;
import com.project666.backend.domain.entity.BillStatusEnum;
import com.project666.backend.domain.message.BillReadyForInsuranceEvent;
import com.project666.backend.repository.AppointmentBillRepository;
import com.project666.backend.repository.LabBillRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BillReadyForInsuranceEventListener {

    private final AppointmentBillRepository appointmentBillRepository;
    private final LabBillRepository labBillRepository;
    private final BillEventPublisher billEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBillCreated(BillReadyForInsuranceEvent event) {
        BaseBill bill = appointmentBillRepository.findById(event.getBillId())
            .map(b -> (BaseBill) b)
            .orElseGet(() -> labBillRepository.findById(event.getBillId())
                .map(b -> (BaseBill) b)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Bill with ID %s not found after commit", event.getBillId())
                )));
        
        if (BillStatusEnum.VIEWING!=bill.getStatus()) throw new IllegalArgumentException(
            String.format("Bill with ID %s is not in viewing status", event.getBillId())
        );

        billEventPublisher.publishBillCreated(bill);
    }
}

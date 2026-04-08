package com.project666.backend.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.project666.backend.config.RabbitConfig;
import com.project666.backend.domain.entity.BaseBill;
import com.project666.backend.domain.message.BillCreatedMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishBillCreated(BaseBill bill) {
        rabbitTemplate.convertAndSend(
            RabbitConfig.BILL_EXCHANGE,
            RabbitConfig.BILL_CREATED_ROUTING_KEY,
            new BillCreatedMessage(
                bill.getId(),
                bill.getAmount(),
                bill.getType().name(),
                bill.getPatient().getId()
            )
        );
    }
}

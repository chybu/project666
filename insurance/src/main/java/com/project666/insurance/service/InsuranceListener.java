package com.project666.insurance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.project666.insurance.config.RabbitConfig;
import com.project666.insurance.message.BillCreatedMessage;
import com.project666.insurance.message.InsuranceCompletedMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InsuranceListener {

    private final RabbitTemplate rabbitTemplate;
    private final Random random = new Random();

    @RabbitListener(queues = RabbitConfig.BILL_CREATED_QUEUE)
    public void handleBillCreated(BillCreatedMessage message) {
        int percent = random.nextInt(61);
        BigDecimal insuranceCoverAmount = message.getAmount()
            .multiply(BigDecimal.valueOf(percent))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal patientPaymentAmount = message.getAmount().subtract(insuranceCoverAmount);

        rabbitTemplate.convertAndSend(
            RabbitConfig.BILL_EXCHANGE,
            RabbitConfig.INSURANCE_COMPLETED_ROUTING_KEY,
            new InsuranceCompletedMessage(
                message.getBillId(),
                insuranceCoverAmount,
                patientPaymentAmount
            )
        );
    }
}

package com.project666.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String BILL_EXCHANGE = "bill.exchange";
    public static final String BILL_CREATED_QUEUE = "insurance.bill.created";
    public static final String INSURANCE_COMPLETED_QUEUE = "billing.insurance.completed";
    public static final String BILL_CREATED_ROUTING_KEY = "bill.created";
    public static final String INSURANCE_COMPLETED_ROUTING_KEY = "insurance.completed";

    @Bean
    public TopicExchange billExchange() {
        return new TopicExchange(BILL_EXCHANGE);
    }

    @Bean
    public Queue billCreatedQueue() {
        return new Queue(BILL_CREATED_QUEUE);
    }

    @Bean
    public Queue insuranceCompletedQueue() {
        return new Queue(INSURANCE_COMPLETED_QUEUE);
    }

    @Bean
    public Binding billCreatedBinding() {
        return BindingBuilder.bind(billCreatedQueue())
            .to(billExchange())
            .with(BILL_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding insuranceCompletedBinding() {
        return BindingBuilder.bind(insuranceCompletedQueue())
            .to(billExchange())
            .with(INSURANCE_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}

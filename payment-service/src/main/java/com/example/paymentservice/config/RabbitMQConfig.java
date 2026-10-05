package com.example.paymentservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "saga.exchange";
    public static final String PAYMENT_COMMANDS_QUEUE = "payment.commands";
    public static final String PAYMENT_COMMANDS_ROUTING_KEY = "payment.command.#";

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue paymentCommandsQueue() {
        return new Queue(PAYMENT_COMMANDS_QUEUE);
    }

    @Bean
    public Binding paymentCommandsBinding(Queue paymentCommandsQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(paymentCommandsQueue).to(sagaExchange).with(PAYMENT_COMMANDS_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

package com.example.inventoryservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String INVENTORY_COMMANDS_QUEUE = "inventory.commands";
    public static final String SAGA_EXCHANGE = "saga.exchange";
    public static final String INVENTORY_ROUTING_KEY = "inventory.command.#";

    @Bean
    public Queue inventoryCommandsQueue() {
        return new Queue(INVENTORY_COMMANDS_QUEUE, true);
    }

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(SAGA_EXCHANGE);
    }

    @Bean
    public Binding inventoryCommandsBinding(Queue inventoryCommandsQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(inventoryCommandsQueue).to(sagaExchange).with(INVENTORY_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

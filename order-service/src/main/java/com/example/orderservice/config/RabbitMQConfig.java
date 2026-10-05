package com.example.orderservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SAGA_EXCHANGE = "saga.exchange";
    public static final String ORDER_REPLIES_QUEUE = "order.replies";
    public static final String ORDER_REPLY_ROUTING_KEY = "order.reply.#";

    @Bean
    public Queue orderRepliesQueue() {
        return new Queue(ORDER_REPLIES_QUEUE, true);
    }

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(SAGA_EXCHANGE);
    }

    @Bean
    public Binding orderRepliesBinding(Queue orderRepliesQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(orderRepliesQueue).to(sagaExchange).with(ORDER_REPLY_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

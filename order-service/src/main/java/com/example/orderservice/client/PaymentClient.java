package com.example.orderservice.client;

import com.example.orderservice.dto.PaymentRequest;
import com.example.orderservice.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public PaymentClient(RestTemplate restTemplate,
                         @Value("${services.payment.url}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("[CLIENT] Processing payment for order: {}", request.getOrderId());
        return restTemplate.postForObject(
                paymentServiceUrl + "/api/payments",
                request,
                PaymentResponse.class
        );
    }

    public void refundPayment(UUID paymentId) {
        log.info("[CLIENT] Refunding payment: {}", paymentId);
        restTemplate.delete(paymentServiceUrl + "/api/payments/" + paymentId + "/refund");
    }
}

package com.gympro.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

// ✅ Handles all payments for bookings and plan subscriptions.
// Dummy payment – simulates CREDIT_CARD / DEBIT_CARD / UPI / QR_CODE
// Calls notification-service via Feign to email payment receipt.
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

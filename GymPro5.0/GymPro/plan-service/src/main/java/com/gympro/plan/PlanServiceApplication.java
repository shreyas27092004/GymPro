package com.gympro.plan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

// ✅ Membership plans – MONTHLY, QUARTERLY, YEARLY
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class PlanServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlanServiceApplication.class, args);
    }
}

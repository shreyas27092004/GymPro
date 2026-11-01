package com.gympro.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * GymPro Chatbot Service – port 8088
 * Provides an AI-powered chatbot endpoint via REST API.
 * Registered with Eureka and routed through the API Gateway.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ChatbotServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotServiceApplication.class, args);
    }
}

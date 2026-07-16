package com.gympro.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * Feign client for trainer-service.
 * Uses Eureka service discovery — works identically in local dev and Docker.
 * Replaces the hardcoded RestTemplate call in AuthService.
 */
@FeignClient(name = "trainer-service")
public interface TrainerServiceClient {

    @PostMapping(value = "/trainers", consumes = MediaType.APPLICATION_JSON_VALUE)
    Object createTrainer(@RequestBody Map<String, Object> trainerPayload,
                         @RequestHeader("X-User-Role") String role);
}
package com.gympro.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    // Used at login time to check whether a TRAINER account has been
    // approved by an admin yet (status field: PENDING | ACTIVE | INACTIVE | REJECTED).
    // Returned as a Map (not a Trainer POJO) so this service doesn't need to
    // duplicate/maintain the full Trainer entity shape.
    @GetMapping("/trainers/by-email/{email}")
    Map<String, Object> getByEmail(@PathVariable("email") String email);
}
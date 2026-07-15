package com.gympro.booking.feign;

import com.gympro.booking.dto.TrainerDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for trainer-service.
 * Used by BookingService to fetch the trainer's sessionFee when payment is required.
 */
@FeignClient(name = "trainer-service")
public interface TrainerServiceClient {

    /**
     * GET /trainers/{id}
     * Returns the Trainer including sessionFee.
     */
    @GetMapping("/trainers/{id}")
    TrainerDto getTrainerById(@PathVariable("id") Long id);
}

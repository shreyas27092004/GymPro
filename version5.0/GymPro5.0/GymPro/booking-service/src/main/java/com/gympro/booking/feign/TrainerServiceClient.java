package com.gympro.booking.feign;

import com.gympro.booking.dto.TrainerDto;
import com.gympro.booking.dto.TrainerScheduleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * Feign client for trainer-service.
 * Used by BookingService to fetch the trainer's sessionFee when payment is
 * required, and to validate/reserve/release a session's capacity.
 */
@FeignClient(name = "trainer-service")
public interface TrainerServiceClient {

    /**
     * GET /trainers/{id}
     * Returns the Trainer including sessionFee.
     */
    @GetMapping("/trainers/{id}")
    TrainerDto getTrainerById(@PathVariable("id") Long id);

    /**
     * GET /trainers/schedule/{scheduleId}
     * Returns the concrete, dated session slot — used to validate
     * capacity, cancellation, and expiry before booking.
     */
    @GetMapping("/trainers/schedule/{scheduleId}")
    TrainerScheduleDto getScheduleById(@PathVariable("scheduleId") Long scheduleId);

    /**
     * PUT /trainers/schedule/{scheduleId}/book
     * Atomically reserves one capacity slot on the session. Trainer-service
     * rejects this (409) if the session is full or cancelled — this is the
     * source-of-truth check that also guards against race conditions.
     */
    @PutMapping("/trainers/schedule/{scheduleId}/book")
    String bookSlot(@PathVariable("scheduleId") Long scheduleId);

    /**
     * PUT /trainers/schedule/{scheduleId}/unbook
     * Releases a previously-reserved capacity slot (called on booking cancellation).
     */
    @PutMapping("/trainers/schedule/{scheduleId}/unbook")
    String unbookSlot(@PathVariable("scheduleId") Long scheduleId);
}

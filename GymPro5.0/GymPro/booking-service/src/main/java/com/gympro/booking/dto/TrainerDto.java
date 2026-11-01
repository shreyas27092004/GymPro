package com.gympro.booking.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight projection of the Trainer entity.
 * Used by TrainerServiceClient to deserialize the trainer-service response
 * inside booking-service. Only the fields needed by BookingService are mapped.
 */
@Data
@NoArgsConstructor
public class TrainerDto {
    private Long id;
    private String name;
    private String email;
    private String specialization;
    private String status;

    /** Per-session fee set by the trainer. May be null if not yet configured. */
    private BigDecimal sessionFee;
}

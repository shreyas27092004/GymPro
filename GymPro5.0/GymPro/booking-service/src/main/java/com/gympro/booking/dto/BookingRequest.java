package com.gympro.booking.dto;

import lombok.Data;

import java.time.LocalDate;

// ✅ What the MEMBER sends to book a session
@Data
public class BookingRequest {
    private Long memberId;
    private String memberEmail;
    private Long trainerId;
    private String trainerEmail;
    private Long scheduleId;
    private String sessionDay;
    private String sessionTime;

    /**
     * The calendar date of the session being booked. Optional from the
     * client's perspective — the authoritative value is always the trainer's
     * dated session slot (fetched via scheduleId in BookingService). If
     * supplied, it is cross-checked against that slot's real date and the
     * request is rejected on a mismatch.
     */
    private LocalDate sessionDate;

    private String notes;
}

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
     * The actual calendar date the member wants the session on.
     * Must be today or a future date, and must fall on the same
     * day-of-week as {@link #sessionDay} (e.g. sessionDay="MON" ⇒
     * sessionDate must be a Monday). Validated server-side in
     * BookingService — never trust the frontend's own validation alone.
     */
    private LocalDate sessionDate;

    private String notes;
}

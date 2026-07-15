package com.gympro.booking.dto;

import lombok.Data;

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
    private String notes;
}

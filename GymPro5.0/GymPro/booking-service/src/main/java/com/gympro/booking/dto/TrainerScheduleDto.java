package com.gympro.booking.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Lightweight projection of trainer-service's TrainerSchedule entity.
 * Used by BookingService (via TrainerServiceClient) to validate a session's
 * date, capacity, and cancellation status before creating a booking.
 */
@Data
@NoArgsConstructor
public class TrainerScheduleDto {
    private Long id;
    private Long trainerId;
    private LocalDate sessionDate;
    private String startTime;
    private String endTime;
    private int maxCapacity;
    private int bookedCount;
    private boolean cancelled;
    private boolean available;
}

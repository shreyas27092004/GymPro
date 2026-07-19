package com.gympro.booking.exception;

/**
 * Thrown when a member tries to book a trainer's schedule slot/date that
 * already has an active (non-cancelled) booking against it.
 */
public class BookingConflictException extends RuntimeException {

    public BookingConflictException(Long trainerId, Long scheduleId, java.time.LocalDate date) {
        super("This slot is already booked for " + date +
              " (trainerId=" + trainerId + ", scheduleId=" + scheduleId +
              "). Please choose a different slot or date.");
    }
}

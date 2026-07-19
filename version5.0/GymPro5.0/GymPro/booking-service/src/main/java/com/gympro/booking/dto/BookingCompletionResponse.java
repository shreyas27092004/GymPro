package com.gympro.booking.dto;

import com.gympro.booking.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API response returned from POST /bookings/complete/{id}.
 *
 * CASE 1 — Booking date is today or in the past (or the admin already
 *          confirmed via confirm=true):
 * {
 *   "success": true,
 *   "confirmationRequired": false,
 *   "bookingId": 42,
 *   "message": "Booking marked as Completed ✅",
 *   "booking": { ...status=COMPLETED... }
 * }
 *
 * CASE 2 — Booking date is in the future AND no confirmation was supplied:
 * {
 *   "success": false,
 *   "confirmationRequired": true,
 *   "bookingId": 42,
 *   "message": "This booking is scheduled for a future date (2026-08-01). Are you sure you want to mark it Completed?",
 *   "booking": null
 * }
 *
 * The booking is NOT modified in CASE 2 — the caller must re-invoke the
 * endpoint with confirm=true (explicit admin confirmation) to proceed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCompletionResponse {

    /** true only when the booking was actually transitioned to COMPLETED. */
    private boolean success;

    /**
     * true = the booking date is in the future; the frontend must show a
     *        warning dialog and re-call this endpoint with confirm=true.
     * false = booking was completed normally (or was already completed).
     */
    private boolean confirmationRequired;

    /** The ID of the booking this response is about — always populated. */
    private Long bookingId;

    /** Human-readable message: either the warning, or a success message. */
    private String message;

    /** The persisted booking record. Null when confirmationRequired=true. */
    private Booking booking;

    // ── Static factory methods ──────────────────────────────────────────

    public static BookingCompletionResponse confirmationRequired(Long bookingId, String warningMessage) {
        return new BookingCompletionResponse(false, true, bookingId, warningMessage, null);
    }

    public static BookingCompletionResponse completed(Booking booking) {
        return new BookingCompletionResponse(
            true, false, booking.getId(), "Booking marked as Completed ✅", booking
        );
    }
}

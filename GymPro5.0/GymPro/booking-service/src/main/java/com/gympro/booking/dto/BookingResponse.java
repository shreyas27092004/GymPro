package com.gympro.booking.dto;

import com.gympro.booking.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * API response returned after creating a booking.
 *
 * CASE 1 — Free session:
 * {
 *   "success": true,
 *   "paymentRequired": false,
 *   "freeSessionUsed": true,
 *   "remainingFreeSessions": 3,
 *   "amount": null,
 *   "booking": { ... }
 * }
 *
 * CASE 2 — Payment required:
 * {
 *   "success": true,
 *   "paymentRequired": true,
 *   "freeSessionUsed": false,
 *   "remainingFreeSessions": 0,
 *   "amount": 500.00,
 *   "booking": { ... }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private boolean success;

    /** true  = frontend must initiate payment (Razorpay or cash)
     *  false = booking is already complete, no payment needed */
    private boolean paymentRequired;

    /** true if this booking consumed a free session from the member's plan */
    private boolean freeSessionUsed;

    /**
     * Free sessions remaining AFTER this booking (already decremented).
     * -1 = unlimited. 0 = none remaining.
     */
    private int remainingFreeSessions;

    /**
     * The trainer's session fee — populated only when paymentRequired=true.
     * Frontend should use this amount to create the Razorpay order or show
     * the cash confirmation dialog.
     */
    private BigDecimal amount;

    /** The persisted booking record */
    private Booking booking;

    // ── Static factory methods ──────────────────────────────────────────

    public static BookingResponse freeSession(Booking booking, int remainingAfter) {
        return new BookingResponse(true, false, true, remainingAfter, null, booking);
    }

    public static BookingResponse paymentRequired(Booking booking, BigDecimal trainerFee) {
        return new BookingResponse(true, true, false, 0, trainerFee, booking);
    }
}

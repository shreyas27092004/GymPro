package com.gympro.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

// ✅ A booking = a MEMBER booked a TRAINER for a session
@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;
    private String memberEmail;

    private Long trainerId;
    private String trainerEmail;

    private Long scheduleId;         // which time slot was booked
    private String sessionDay;       // e.g. "MON"
    private String sessionTime;      // e.g. "09:00 - 11:00"

    /**
     * The actual calendar date of the training session (chosen by the member
     * from the calendar picker on the frontend). Must fall on the same
     * day-of-week as {@link #sessionDay} and cannot be in the past —
     * enforced in BookingService.validateBookingRequest().
     */
    private LocalDate bookingDate;

    /** When the booking record itself was created (audit trail, not the session date). */
    private LocalDateTime createdAt;

    private String status;           // CONFIRMED | CANCELLED | COMPLETED
    private String notes;

    // ── NEW fields for free-session tracking ─────────────────────────────

    /**
     * FREE_SESSION = covered by membership plan (no payment needed)
     * PAID         = member must pay / has paid
     */
    @Column(name = "session_type", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'PAID'")
    private String sessionType;      // FREE_SESSION | PAID

    /**
     * Payment status for this booking.
     * FREE_SESSION bookings: COMPLETED (no payment gateway involved)
     * PAID bookings:         PENDING → SUCCESS (after payment-service confirms)
     */
    @Column(name = "payment_status", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    private String paymentStatus;    // PENDING | COMPLETED | FAILED
}

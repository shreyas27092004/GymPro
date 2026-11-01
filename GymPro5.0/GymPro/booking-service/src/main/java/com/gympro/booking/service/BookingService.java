package com.gympro.booking.service;

import com.gympro.booking.dto.BookingCompletionResponse;
import com.gympro.booking.dto.BookingRequest;
import com.gympro.booking.dto.BookingResponse;
import com.gympro.booking.dto.FreeSessionCheckResult;
import com.gympro.booking.dto.NotificationEvent;
import com.gympro.booking.dto.TrainerDto;
import com.gympro.booking.dto.TrainerScheduleDto;
import com.gympro.booking.entity.Booking;
import com.gympro.booking.exception.BookingConflictException;
import com.gympro.booking.exception.BookingNotFoundException;
import com.gympro.booking.feign.PlanServiceClient;
import com.gympro.booking.feign.TrainerServiceClient;
import com.gympro.booking.messaging.NotificationPublisher;
import com.gympro.booking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import java.util.List;

// ─────────────────────────────────────────────────────────────────────────────
//  BookingService — complete free-session + payment logic + session validation
//
//  Booking creation flow:
//   1. Validate request fields, and fetch the trainer's dated session slot
//      (trainer-service) to validate it: not cancelled, not expired, not
//      full, and not already booked by this member.
//   2. Reject double-booking of the same trainer slot/date (source-of-truth
//      check against this service's own booking table).
//   3. Reserve a capacity slot on the session (trainer-service, atomic
//      source-of-truth check — also guards against race conditions).
//   4. Fetch FreeSessionCheckResult from plan-service (read-only Feign).
//   5a. isFree=true  → consume session via plan-service, save FREE_SESSION booking.
//   5b. isFree=false → fetch trainer fee, save PAID booking (PENDING payment).
//   6. Publish async RabbitMQ notification.
//   If any step after the capacity reservation fails, the reserved slot is
//   released so it isn't lost.
//
//  Cancellation flow:
//   - FREE_SESSION bookings → restore free session via plan-service.
//   - PAID bookings         → refund handled by payment-service.
//   - ALL bookings          → release the reserved capacity slot on the session.
//
//  Completion flow:
//   - Future-dated bookings require explicit admin confirmation before being
//     marked COMPLETED (see completeBooking).
//
//  Security:
//   - ALL free session decisions are made on the BACKEND (plan-service).
//   - Frontend cannot bypass payment by sending sessionType=FREE_SESSION directly.
//   - Trainer fee is fetched from trainer-service, not trusted from client.
//   - Session capacity/cancellation/expiry are fetched from trainer-service,
//     not trusted from client.
// ─────────────────────────────────────────────────────────────────────────────
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    @Autowired private BookingRepository      repo;
    @Autowired private NotificationPublisher  notificationPublisher;
    @Autowired private PlanServiceClient      planServiceClient;
    @Autowired private TrainerServiceClient   trainerServiceClient;
    @Autowired private com.gympro.booking.feign.AuthServiceClient authServiceClient;

    /**
     * Best-effort fetch of every ADMIN-role user ID, for broadcasting admin
     * in-app notifications. Never throws — admin broadcast is a "nice to have"
     * and must not block the member/trainer notification flow if auth-service
     * is briefly unreachable.
     */
    private java.util.List<Long> fetchAdminUserIds() {
        try {
            return authServiceClient.getAdminUserIds();
        } catch (Exception e) {
            log.warn("⚠️ Could not fetch admin user IDs for broadcast notification: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CREATE BOOKING
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Main entry point. Decides FREE vs PAID entirely on the backend.
     * Frontend CANNOT influence the sessionType — it is always set here.
     *
     * @return BookingResponse with paymentRequired flag and remaining free-session count.
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest req) {
        validateBookingRequest(req);

        // ── Step 0: Reject double-booking of the same trainer slot/date ─────
        // Two members could otherwise both pass this check for the same slot
        // in quick succession; @Transactional narrows but doesn't fully close
        // that window (see class-level notes / remaining tech debt).
        if (repo.existsByTrainerIdAndScheduleIdAndBookingDateAndStatusNot(
                req.getTrainerId(), req.getScheduleId(), req.getSessionDate(), "CANCELLED")) {
            throw new BookingConflictException(req.getTrainerId(), req.getScheduleId(), req.getSessionDate());
        }

        // ── Reserve a capacity slot on the session (source of truth) ────────
        reserveScheduleCapacity(req.getScheduleId());

        try {
            // ── Step 1: Check free session eligibility (read-only) ──────────
            FreeSessionCheckResult freeCheck = fetchFreeSessionCheck(req.getMemberId());

            // ── Step 2a: FREE SESSION path ───────────────────────────────────
            if (freeCheck.isFree()) {
                return handleFreeSessionBooking(req, freeCheck);
            }

            // ── Step 2b: PAID SESSION path ───────────────────────────────────
            // A member with NO active subscription who has already used their
            // one-time lifetime free session (subscriptionId == null here)
            // must not be allowed to keep booking paid sessions indefinitely —
            // they need to take at least a base membership plan first.
            if (freeCheck.getSubscriptionId() == null) {
                throw new IllegalStateException(
                    "You've used your one-time free session. Please subscribe to a membership plan " +
                    "to book further trainer sessions."
                );
            }
            return handlePaidSessionBooking(req);
        } catch (RuntimeException ex) {
            // The booking did not complete — give the capacity slot back.
            releaseScheduleCapacityQuietly(req.getScheduleId());
            throw ex;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET METHODS
    // ════════════════════════════════════════════════════════════════════════

    public Booking getBookingById(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new BookingNotFoundException(id));
    }

    public List<Booking> getBookingsByMember(Long memberId) {
        return repo.findByMemberId(memberId);
    }

    public List<Booking> getBookingsByTrainer(Long trainerId) {
        return repo.findByTrainerId(trainerId);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CANCEL BOOKING
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Cancels a booking.
     * - FREE_SESSION bookings: restores the consumed free session (subscribed or not).
     * - PAID bookings: refund is handled separately through payment-service.
     * - ALL bookings: releases the reserved capacity slot on the trainer's session.
     */
    public String cancelBooking(Long id) {
        Booking booking = getBookingById(id);

        if ("CANCELLED".equals(booking.getStatus())) {
            return "Booking already cancelled";
        }

        booking.setStatus("CANCELLED");
        repo.save(booking);
        log.info("✅ Booking cancelled: id={}", id);

        // ── Restore free session if this was a free booking ─────────────────
        if ("FREE_SESSION".equals(booking.getSessionType())) {
            try {
                planServiceClient.restoreFreeSession(booking.getMemberId());
                log.info("✅ Free session restored for memberId={} after booking cancellation", booking.getMemberId());
            } catch (Exception e) {
                // Log but don't fail — booking is already cancelled
                log.warn("⚠️ Failed to restore free session for memberId={}: {}", booking.getMemberId(), e.getMessage());
            }
        }

        // ── Release the capacity slot on the session ─────────────────────────
        releaseScheduleCapacityQuietly(booking.getScheduleId());

        // ── Async notification ───────────────────────────────────────────────
        try {
            NotificationEvent cancelledEvent = NotificationEvent.bookingCancelled(
                booking.getMemberEmail(), booking.getMemberId(), id);
            cancelledEvent.setAdminUserIds(fetchAdminUserIds());
            notificationPublisher.publishBookingEvent(cancelledEvent);
        } catch (Exception e) {
            log.warn("⚠️ Unexpected error while publishing cancellation notification: {}", e.getMessage());
        }

        return "Booking cancelled ✅";
    }

    // ════════════════════════════════════════════════════════════════════════
    //  COMPLETE BOOKING
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Marks a booking as COMPLETED.
     *
     * Validation:
     *  - If the booking's session date ({@link Booking#getBookingDate()}) is in
     *    the FUTURE (strictly after today) and the caller has not explicitly
     *    confirmed ({@code confirmed=false}), the booking is left untouched and
     *    a "confirmation required" response is returned instead, carrying the
     *    booking ID and a warning message for the frontend to display.
     *  - Past or today-dated bookings complete normally, no confirmation needed.
     *  - Once the admin explicitly confirms ({@code confirmed=true}), the
     *    booking is completed regardless of its date.
     *
     * @param id        the booking ID
     * @param confirmed whether the caller has already confirmed completion of
     *                  a future-dated booking (defaults to false at the API layer)
     */
    public BookingCompletionResponse completeBooking(Long id, boolean confirmed) {
        Booking booking = getBookingById(id);

        if (isFutureBooking(booking) && !confirmed) {
            String warning = "This booking is scheduled for a future date ("
                + booking.getBookingDate() + "). Completing it now means marking a "
                + "session as done before it has actually taken place. Please confirm "
                + "you still want to mark booking #" + id + " as Completed.";
            log.info("⚠️ completeBooking requires confirmation: id={}, bookingDate={}", id, booking.getBookingDate());
            return BookingCompletionResponse.confirmationRequired(id, warning);
        }

        booking.setStatus("COMPLETED");
        Booking saved = repo.save(booking);
        log.info("✅ Booking marked COMPLETED: id={}", id);
        return BookingCompletionResponse.completed(saved);
    }

    /** Convenience overload — no explicit confirmation supplied (confirmed=false). */
    public BookingCompletionResponse completeBooking(Long id) {
        return completeBooking(id, false);
    }

    /** True when the booking's session date is strictly after today. Null dates are never "future". */
    private boolean isFutureBooking(Booking booking) {
        LocalDate bookingDate = booking.getBookingDate();
        return bookingDate != null && bookingDate.isAfter(LocalDate.now());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CONFIRM PAYMENT  (called by payment-service after successful payment)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Called by payment-service (or via an internal admin API) after a
     * successful Razorpay/cash payment to mark the booking's paymentStatus=COMPLETED.
     *
     * Prevents payment bypass: a PAID booking stays in PENDING state until
     * this is explicitly called with a verified payment.
     */
    public Booking confirmPayment(Long bookingId) {
        Booking booking = getBookingById(bookingId);

        if (!"PAID".equals(booking.getSessionType())) {
            throw new IllegalStateException(
                "Booking #" + bookingId + " is a FREE_SESSION — no payment confirmation needed."
            );
        }

        if ("COMPLETED".equals(booking.getPaymentStatus())) {
            log.warn("⚠️ Payment already confirmed for bookingId={}", bookingId);
            return booking;
        }

        booking.setPaymentStatus("COMPLETED");
        Booking saved = repo.save(booking);
        log.info("✅ Payment confirmed for bookingId={}", bookingId);
        return saved;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private BookingResponse handleFreeSessionBooking(BookingRequest req, FreeSessionCheckResult freeCheck) {
        // Atomically consume one free session in plan-service
        boolean consumed = false;
        try {
            consumed = planServiceClient.useIncludedSession(req.getMemberId());
        } catch (Exception e) {
            log.warn("⚠️ Could not consume free session via plan-service: {}. Falling back to PAID.", e.getMessage());
        }

        if (!consumed) {
            // Race condition or plan-service down — fall back to paid
            log.warn("⚠️ Free session check positive but consume returned false for memberId={}. Treating as PAID.",
                req.getMemberId());
            return handlePaidSessionBooking(req);
        }

        // Build and persist the FREE_SESSION booking
        Booking booking = buildBooking(req);
        booking.setSessionType("FREE_SESSION");
        booking.setPaymentStatus("COMPLETED");   // No payment needed
        Booking saved = repo.save(booking);
        log.info("✅ FREE_SESSION booking saved: id={}, memberId={}", saved.getId(), req.getMemberId());

        // Publish async notifications
        publishBookingNotifications(saved, req);

        // remaining = was N before consume, now N-1 (unlimited -1 stays -1)
        int remainingAfter = freeCheck.getRemainingFreeSessions() == -1
            ? -1
            : Math.max(0, freeCheck.getRemainingFreeSessions() - 1);

        return BookingResponse.freeSession(saved, remainingAfter);
    }

    private BookingResponse handlePaidSessionBooking(BookingRequest req) {
        // Trainer fee is fetched from backend — not trusted from client
        BigDecimal trainerFee = fetchTrainerFee(req.getTrainerId());

        if (trainerFee == null || trainerFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                "Trainer #" + req.getTrainerId() + " has not configured a session fee. " +
                "Please ask the trainer to set their session fee before booking."
            );
        }

        // PAID bookings start with paymentStatus=PENDING — confirmed only after payment
        Booking booking = buildBooking(req);
        booking.setSessionType("PAID");
        booking.setPaymentStatus("PENDING");
        Booking saved = repo.save(booking);
        log.info("✅ PAID booking saved: id={}, memberId={}, fee=₹{}", saved.getId(), req.getMemberId(), trainerFee);

        // Publish async notifications
        publishBookingNotifications(saved, req);

        return BookingResponse.paymentRequired(saved, trainerFee);
    }

    private Booking buildBooking(BookingRequest req) {
        Booking b = new Booking();
        b.setMemberId(req.getMemberId());
        b.setMemberEmail(req.getMemberEmail());
        b.setTrainerId(req.getTrainerId());
        b.setTrainerEmail(req.getTrainerEmail());
        b.setScheduleId(req.getScheduleId());
        b.setSessionDay(req.getSessionDay());
        b.setSessionTime(req.getSessionTime());
        b.setBookingDate(req.getSessionDate());
        b.setCreatedAt(LocalDateTime.now());
        b.setStatus("CONFIRMED");
        b.setNotes(req.getNotes());
        // sessionType and paymentStatus are set by the calling method
        return b;
    }

    private FreeSessionCheckResult fetchFreeSessionCheck(Long memberId) {
        try {
            return planServiceClient.checkFreeSession(memberId);
        } catch (Exception e) {
            log.warn("⚠️ plan-service unreachable for free-session check, defaulting to PAID. Error: {}", e.getMessage());
            // Safe default: treat as no free sessions when plan-service is down.
            // Sentinel subscriptionId (-1) marks this as an OUTAGE fallback, not a
            // genuine "no subscription" result — so the no-subscription booking
            // guard below doesn't misfire and block members who actually do have
            // an active plan during a transient plan-service hiccup.
            FreeSessionCheckResult fallback = new FreeSessionCheckResult();
            fallback.setFree(false);
            fallback.setRemainingFreeSessions(0);
            fallback.setSubscriptionId(-1L);
            return fallback;
        }
    }

    private BigDecimal fetchTrainerFee(Long trainerId) {
        try {
            TrainerDto trainer = trainerServiceClient.getTrainerById(trainerId);
            return trainer != null ? trainer.getSessionFee() : null;
        } catch (Exception e) {
            log.warn("⚠️ trainer-service unreachable for fee lookup, trainerId={}: {}", trainerId, e.getMessage());
            return null;
        }
    }

    private void publishBookingNotifications(Booking saved, BookingRequest req) {
        try {
            NotificationEvent confirmedEvent = NotificationEvent.bookingConfirmed(
                req.getMemberEmail(), req.getMemberId(), saved.getId(),
                "Trainer #" + req.getTrainerId(),
                req.getSessionDay(), req.getSessionTime()
            );
            confirmedEvent.setAdminUserIds(fetchAdminUserIds());
            notificationPublisher.publishBookingEvent(confirmedEvent);

            notificationPublisher.publishBookingEvent(
                NotificationEvent.bookingToTrainer(
                    req.getTrainerEmail(), req.getTrainerId(), saved.getId(),
                    "Member #" + req.getMemberId(),
                    req.getSessionDay(), req.getSessionTime()
                )
            );
        } catch (Exception e) {
            log.warn("⚠️ Unexpected error while publishing booking notification: {}", e.getMessage());
        }
    }

    // ── Session slot validation (calendar-based scheduling) ──────────────────

    private void validateBookingRequest(BookingRequest req) {
        if (req.getMemberId() == null)      throw new IllegalArgumentException("memberId is required");
        if (req.getMemberEmail() == null)   throw new IllegalArgumentException("memberEmail is required");
        if (req.getTrainerId() == null)     throw new IllegalArgumentException("trainerId is required");
        if (req.getScheduleId() == null)    throw new IllegalArgumentException("scheduleId is required");

        // ── Fetch the trainer's concrete, dated session slot ─────────────────
        // This is the authoritative source for the session's date/time,
        // capacity, and cancellation status — never trust the client alone.
        TrainerScheduleDto schedule = fetchSchedule(req.getScheduleId());

        if (schedule.getTrainerId() != null && !schedule.getTrainerId().equals(req.getTrainerId())) {
            throw new IllegalArgumentException(
                "scheduleId " + req.getScheduleId() + " does not belong to trainerId " + req.getTrainerId());
        }

        // ── Reject: cancelled session ─────────────────────────────────────────
        if (schedule.isCancelled()) {
            throw new IllegalStateException(
                "This session has been cancelled by the trainer and can no longer be booked");
        }

        // ── Reject: expired session ───────────────────────────────────────────
        LocalTime endTime = parseTime(schedule.getEndTime());
        LocalDateTime sessionEnds = LocalDateTime.of(schedule.getSessionDate(), endTime);
        if (sessionEnds.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("This session has already ended and can no longer be booked");
        }

        // ── Reject: session full ──────────────────────────────────────────────
        // Defensive check — the authoritative, race-condition-safe check
        // happens again in trainer-service when the capacity slot is reserved.
        if (schedule.getBookedCount() >= schedule.getMaxCapacity()) {
            throw new IllegalStateException(
                "This session is already full (capacity: " + schedule.getMaxCapacity() + ")");
        }

        // ── Reject: duplicate booking ─────────────────────────────────────────
        // Same member cannot hold two active (CONFIRMED) bookings on the same session.
        if (repo.existsByMemberIdAndScheduleIdAndStatus(req.getMemberId(), req.getScheduleId(), "CONFIRMED")) {
            throw new IllegalStateException("You have already booked this session");
        }

        // The session's real calendar date comes from trainer-service. If the
        // client also supplied one, it must agree with it (defence in depth);
        // either way the authoritative value is written back onto the request.
        if (req.getSessionDate() != null && !req.getSessionDate().equals(schedule.getSessionDate())) {
            throw new IllegalArgumentException(
                "sessionDate (" + req.getSessionDate() + ") does not match this session's scheduled date (" +
                schedule.getSessionDate() + ")"
            );
        }
        req.setSessionDate(schedule.getSessionDate());
    }

    private LocalTime parseTime(String time) {
        try {
            return LocalTime.parse(time);
        } catch (DateTimeParseException | NullPointerException e) {
            // If trainer-service ever returns a malformed time, treat the
            // session conservatively as not-yet-expired rather than crashing.
            return LocalTime.MAX;
        }
    }

    private TrainerScheduleDto fetchSchedule(Long scheduleId) {
        TrainerScheduleDto schedule;
        try {
            schedule = trainerServiceClient.getScheduleById(scheduleId);
        } catch (Exception e) {
            log.warn("⚠️ trainer-service unreachable while fetching scheduleId={}: {}", scheduleId, e.getMessage());
            throw new IllegalStateException("Could not verify session details right now. Please try again.");
        }
        if (schedule == null) {
            throw new IllegalArgumentException("Session slot #" + scheduleId + " was not found");
        }
        return schedule;
    }

    private void reserveScheduleCapacity(Long scheduleId) {
        try {
            trainerServiceClient.bookSlot(scheduleId);
        } catch (Exception e) {
            log.warn("⚠️ Could not reserve a capacity slot for scheduleId={}: {}", scheduleId, e.getMessage());
            throw new IllegalStateException(
                "Could not reserve your spot for this session — it may have just become full or was cancelled. " +
                "Please try again or choose another session.");
        }
    }

    private void releaseScheduleCapacityQuietly(Long scheduleId) {
        try {
            trainerServiceClient.unbookSlot(scheduleId);
        } catch (Exception e) {
            log.warn("⚠️ Failed to release the capacity slot for scheduleId={}: {}", scheduleId, e.getMessage());
        }
    }
}

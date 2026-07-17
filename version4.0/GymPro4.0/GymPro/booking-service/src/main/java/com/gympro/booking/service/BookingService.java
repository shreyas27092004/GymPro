package com.gympro.booking.service;

import com.gympro.booking.dto.BookingRequest;
import com.gympro.booking.dto.BookingResponse;
import com.gympro.booking.dto.FreeSessionCheckResult;
import com.gympro.booking.dto.NotificationEvent;
import com.gympro.booking.dto.TrainerDto;
import com.gympro.booking.entity.Booking;
import com.gympro.booking.exception.BookingNotFoundException;
import com.gympro.booking.feign.PlanServiceClient;
import com.gympro.booking.feign.TrainerServiceClient;
import com.gympro.booking.messaging.NotificationPublisher;
import com.gympro.booking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// ─────────────────────────────────────────────────────────────────────────────
//  BookingService — complete free-session + payment logic
//
//  Booking creation flow:
//   1. Validate request fields.
//   2. Fetch FreeSessionCheckResult from plan-service (read-only Feign).
//   3a. isFree=true  → consume session via plan-service, save FREE_SESSION booking.
//   3b. isFree=false → fetch trainer fee, save PAID booking (PENDING payment).
//   4. Publish async RabbitMQ notification.
//
//  Cancellation flow:
//   - FREE_SESSION bookings → restore free session via plan-service.
//   - PAID bookings         → refund handled by payment-service.
//
//  Security:
//   - ALL free session decisions are made on the BACKEND (plan-service).
//   - Frontend cannot bypass payment by sending sessionType=FREE_SESSION directly.
//   - Trainer fee is fetched from trainer-service, not trusted from client.
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
    public BookingResponse createBooking(BookingRequest req) {
        validateBookingRequest(req);

        // ── Step 1: Check free session eligibility (read-only) ──────────────
        FreeSessionCheckResult freeCheck = fetchFreeSessionCheck(req.getMemberId());

        // ── Step 2a: FREE SESSION path ──────────────────────────────────────
        if (freeCheck.isFree()) {
            return handleFreeSessionBooking(req, freeCheck);
        }

        // ── Step 2b: PAID SESSION path ──────────────────────────────────────
        return handlePaidSessionBooking(req);
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

    public Booking completeBooking(Long id) {
        Booking booking = getBookingById(id);
        booking.setStatus("COMPLETED");
        return repo.save(booking);
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
            // Safe default: treat as no free sessions when plan-service is down
            FreeSessionCheckResult fallback = new FreeSessionCheckResult();
            fallback.setFree(false);
            fallback.setRemainingFreeSessions(0);
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

    /** Maps java.time.DayOfWeek to the 3-letter codes used by TrainerSchedule (MON, TUE, ...). */
    private static final Map<DayOfWeek, String> DAY_CODES = Map.of(
        DayOfWeek.MONDAY,    "MON",
        DayOfWeek.TUESDAY,   "TUE",
        DayOfWeek.WEDNESDAY, "WED",
        DayOfWeek.THURSDAY,  "THU",
        DayOfWeek.FRIDAY,    "FRI",
        DayOfWeek.SATURDAY,  "SAT",
        DayOfWeek.SUNDAY,    "SUN"
    );

    private void validateBookingRequest(BookingRequest req) {
        if (req.getMemberId() == null)      throw new IllegalArgumentException("memberId is required");
        if (req.getMemberEmail() == null)   throw new IllegalArgumentException("memberEmail is required");
        if (req.getTrainerId() == null)     throw new IllegalArgumentException("trainerId is required");
        if (req.getScheduleId() == null)    throw new IllegalArgumentException("scheduleId is required");

        // ── Session date validation ──────────────────────────────────────────
        // A member must pick an actual calendar date for the session — never
        // trust a missing date or one supplied only by client-side JS checks.
        if (req.getSessionDate() == null) {
            throw new IllegalArgumentException("sessionDate is required — please choose a date for the session");
        }

        if (req.getSessionDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("sessionDate cannot be in the past");
        }

        // The chosen date must actually fall on the slot's day-of-week
        // (e.g. sessionDay="MON" ⇒ sessionDate must be a Monday). This
        // guards against a tampered/mismatched request even if the frontend
        // calendar only ever offers valid dates.
        if (req.getSessionDay() != null) {
            String expectedCode = DAY_CODES.get(req.getSessionDate().getDayOfWeek());
            if (!req.getSessionDay().equalsIgnoreCase(expectedCode)) {
                throw new IllegalArgumentException(
                    "sessionDate (" + req.getSessionDate() + ") does not fall on the slot's day (" +
                    req.getSessionDay() + ")"
                );
            }
        }
    }
}

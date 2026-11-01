package com.gympro.booking;

import com.gympro.booking.dto.BookingRequest;
import com.gympro.booking.dto.BookingResponse;
import com.gympro.booking.dto.FreeSessionCheckResult;
import com.gympro.booking.dto.NotificationEvent;
import com.gympro.booking.dto.TrainerDto;
import com.gympro.booking.dto.TrainerScheduleDto;
import com.gympro.booking.entity.Booking;
import com.gympro.booking.exception.BookingNotFoundException;
import com.gympro.booking.feign.PlanServiceClient;
import com.gympro.booking.feign.TrainerServiceClient;
import com.gympro.booking.messaging.NotificationPublisher;
import com.gympro.booking.repository.BookingRepository;
import com.gympro.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService — free session and payment rules")
public class BookingServiceTest {

    @Mock private BookingRepository     bookingRepository;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private PlanServiceClient     planServiceClient;
    @Mock private TrainerServiceClient  trainerServiceClient;

    @InjectMocks
    private BookingService bookingService;

    private BookingRequest sampleRequest;
    private Booking        sampleBooking;
    private TrainerDto     trainerWithFee;

    @BeforeEach
    void setUp() {
        sampleRequest = new BookingRequest();
        sampleRequest.setMemberId(10L);
        sampleRequest.setMemberEmail("member@gym.com");
        sampleRequest.setTrainerId(5L);
        sampleRequest.setTrainerEmail("trainer@gym.com");
        sampleRequest.setScheduleId(3L);
        sampleRequest.setSessionDay("MON");
        sampleRequest.setSessionTime("09:00 - 11:00");
        sampleRequest.setNotes("Test booking");

        sampleBooking = new Booking();
        sampleBooking.setId(1L);
        sampleBooking.setMemberId(10L);
        sampleBooking.setMemberEmail("member@gym.com");
        sampleBooking.setTrainerId(5L);
        sampleBooking.setStatus("CONFIRMED");
        sampleBooking.setSessionType("PAID");
        sampleBooking.setPaymentStatus("PENDING");
        sampleBooking.setBookingDate(LocalDate.now());

        trainerWithFee = new TrainerDto();
        trainerWithFee.setId(5L);
        trainerWithFee.setSessionFee(new BigDecimal("500.00"));

        // ── Calendar-based session slot backing scheduleId=3 (trainer #5) ──
        // Used by validateBookingRequest/reserveScheduleCapacity in every
        // createBooking() test. lenient() because some tests (null-field
        // validation) never reach the schedule lookup at all.
        TrainerScheduleDto validSchedule = new TrainerScheduleDto();
        validSchedule.setId(3L);
        validSchedule.setTrainerId(5L);
        validSchedule.setSessionDate(LocalDate.now().plusDays(1));
        validSchedule.setStartTime("09:00");
        validSchedule.setEndTime("11:00");
        validSchedule.setMaxCapacity(20);
        validSchedule.setBookedCount(0);
        validSchedule.setCancelled(false);
        validSchedule.setAvailable(true);
        lenient().when(trainerServiceClient.getScheduleById(3L)).thenReturn(validSchedule);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  NON-SUBSCRIBED MEMBER — 1 free session
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Non-subscribed member — 1 lifetime free session")
    class NonSubscribedMember {

        @Test
        @DisplayName("First booking is FREE when member has no subscription and has not used free session")
        void firstBooking_noSubscription_isFree() {
            // plan-service says: no subscription, free session available (remaining=1)
            FreeSessionCheckResult freeCheck = new FreeSessionCheckResult(true, 1, 1, null);
            when(planServiceClient.checkFreeSession(10L)).thenReturn(freeCheck);
            when(planServiceClient.useIncludedSession(10L)).thenReturn(true);

            Booking savedBooking = buildSavedBooking("FREE_SESSION", "COMPLETED");
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            BookingResponse response = bookingService.createBooking(sampleRequest);

            assertTrue(response.isSuccess());
            assertFalse(response.isPaymentRequired(),    "First booking must NOT require payment");
            assertTrue(response.isFreeSessionUsed(),     "Free session must be marked as used");
            assertEquals(0, response.getRemainingFreeSessions(), "No free sessions should remain after using the only one");
            assertNull(response.getAmount(),             "Amount must be null for free session");

            verify(planServiceClient).useIncludedSession(10L);
            verify(bookingRepository).save(any(Booking.class));
            // Trainer fee should NOT be fetched for free bookings
            verify(trainerServiceClient, never()).getTrainerById(anyLong());
        }

        @Test
        @DisplayName("Second booking requires PAYMENT after free session is used")
        void secondBooking_noSubscription_requiresPayment() {
            // plan-service says: free session already used
            FreeSessionCheckResult paidCheck = new FreeSessionCheckResult(false, 0, 1, null);
            when(planServiceClient.checkFreeSession(10L)).thenReturn(paidCheck);
            when(trainerServiceClient.getTrainerById(5L)).thenReturn(trainerWithFee);

            Booking savedBooking = buildSavedBooking("PAID", "PENDING");
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            BookingResponse response = bookingService.createBooking(sampleRequest);

            assertTrue(response.isSuccess());
            assertTrue(response.isPaymentRequired(),    "Second booking MUST require payment");
            assertFalse(response.isFreeSessionUsed(),  "Free session must NOT be marked as used");
            assertEquals(new BigDecimal("500.00"), response.getAmount(), "Amount must equal trainer fee");

            verify(planServiceClient, never()).useIncludedSession(anyLong());
            verify(trainerServiceClient).getTrainerById(5L);
        }

        @Test
        @DisplayName("Cancelling a free booking restores the free session")
        void cancelFreeBooking_restoresFreeSession() {
            Booking freeBooking = buildSavedBooking("FREE_SESSION", "COMPLETED");
            freeBooking.setId(1L);
            freeBooking.setMemberId(10L);

            when(bookingRepository.findById(1L)).thenReturn(Optional.of(freeBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(freeBooking);
            when(planServiceClient.restoreFreeSession(10L)).thenReturn("Free session restored ✅");

            String result = bookingService.cancelBooking(1L);

            assertEquals("Booking cancelled ✅", result);
            verify(planServiceClient).restoreFreeSession(10L);
        }

        @Test
        @DisplayName("Cancelling a PAID booking does NOT restore a free session")
        void cancelPaidBooking_doesNotRestoreFreeSession() {
            Booking paidBooking = buildSavedBooking("PAID", "PENDING");
            paidBooking.setId(2L);
            paidBooking.setMemberId(10L);

            when(bookingRepository.findById(2L)).thenReturn(Optional.of(paidBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(paidBooking);

            bookingService.cancelBooking(2L);

            verify(planServiceClient, never()).restoreFreeSession(anyLong());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SUBSCRIBED MEMBER — sessionsIncluded from plan
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Subscribed member — plan free session quota")
    class SubscribedMember {

        @Test
        @DisplayName("Booking is free when subscribed member has remaining sessions")
        void booking_withRemainingFreeSessions_isFree() {
            // subscribed, 3 remaining free sessions
            FreeSessionCheckResult freeCheck = new FreeSessionCheckResult(true, 3, 5, 99L);
            when(planServiceClient.checkFreeSession(10L)).thenReturn(freeCheck);
            when(planServiceClient.useIncludedSession(10L)).thenReturn(true);

            Booking savedBooking = buildSavedBooking("FREE_SESSION", "COMPLETED");
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            BookingResponse response = bookingService.createBooking(sampleRequest);

            assertFalse(response.isPaymentRequired());
            assertTrue(response.isFreeSessionUsed());
            assertEquals(2, response.getRemainingFreeSessions(), "Should have 2 sessions left after using 1 of 3");
            verify(planServiceClient).useIncludedSession(10L);
        }

        @Test
        @DisplayName("Booking requires payment when all free sessions are exhausted")
        void booking_withNoFreeSessions_requiresPayment() {
            FreeSessionCheckResult exhausted = new FreeSessionCheckResult(false, 0, 5, 99L);
            when(planServiceClient.checkFreeSession(10L)).thenReturn(exhausted);
            when(trainerServiceClient.getTrainerById(5L)).thenReturn(trainerWithFee);

            Booking savedBooking = buildSavedBooking("PAID", "PENDING");
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            BookingResponse response = bookingService.createBooking(sampleRequest);

            assertTrue(response.isPaymentRequired());
            assertEquals(new BigDecimal("500.00"), response.getAmount());
        }

        @Test
        @DisplayName("Unlimited sessions plan (sessionsIncluded=-1) never requires payment")
        void booking_unlimitedPlan_alwaysFree() {
            FreeSessionCheckResult unlimited = new FreeSessionCheckResult(true, -1, -1, 99L);
            when(planServiceClient.checkFreeSession(10L)).thenReturn(unlimited);
            when(planServiceClient.useIncludedSession(10L)).thenReturn(true);

            Booking savedBooking = buildSavedBooking("FREE_SESSION", "COMPLETED");
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            BookingResponse response = bookingService.createBooking(sampleRequest);

            assertFalse(response.isPaymentRequired());
            assertEquals(-1, response.getRemainingFreeSessions(), "Unlimited should report -1 remaining");
        }

        @Test
        @DisplayName("Zero sessions plan (sessionsIncluded=0) always requires payment")
        void booking_zeroSessionPlan_requiresPayment() {
            FreeSessionCheckResult zero = new FreeSessionCheckResult(false, 0, 0, 99L);
            when(planServiceClient.checkFreeSession(10L)).thenReturn(zero);
            when(trainerServiceClient.getTrainerById(5L)).thenReturn(trainerWithFee);

            Booking savedBooking = buildSavedBooking("PAID", "PENDING");
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            BookingResponse response = bookingService.createBooking(sampleRequest);

            assertTrue(response.isPaymentRequired());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PAYMENT VALIDATION
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Payment validation — backend-enforced")
    class PaymentValidation {

        @Test
        @DisplayName("Throws exception when trainer has no session fee configured")
        void booking_trainerHasNoFee_throwsException() {
            FreeSessionCheckResult paidCheck = new FreeSessionCheckResult(false, 0, 0, null);
            when(planServiceClient.checkFreeSession(10L)).thenReturn(paidCheck);

            TrainerDto trainerNoFee = new TrainerDto();
            trainerNoFee.setId(5L);
            trainerNoFee.setSessionFee(null); // fee not configured
            when(trainerServiceClient.getTrainerById(5L)).thenReturn(trainerNoFee);

            assertThrows(IllegalStateException.class, () -> bookingService.createBooking(sampleRequest),
                "Should throw when trainer has no fee configured");
        }

        @Test
        @DisplayName("Throws exception when trainer fee is zero")
        void booking_trainerFeeIsZero_throwsException() {
            FreeSessionCheckResult paidCheck = new FreeSessionCheckResult(false, 0, 0, null);
            when(planServiceClient.checkFreeSession(10L)).thenReturn(paidCheck);

            TrainerDto trainerZeroFee = new TrainerDto();
            trainerZeroFee.setId(5L);
            trainerZeroFee.setSessionFee(BigDecimal.ZERO);
            when(trainerServiceClient.getTrainerById(5L)).thenReturn(trainerZeroFee);

            assertThrows(IllegalStateException.class, () -> bookingService.createBooking(sampleRequest),
                "Should throw when trainer fee is zero");
        }

        @Test
        @DisplayName("Falls back to PAID path when plan-service is unreachable")
        void booking_planServiceDown_defaultsToPaid() {
            // plan-service throws a network exception
            when(planServiceClient.checkFreeSession(anyLong()))
                .thenThrow(new RuntimeException("Connection refused"));
            when(trainerServiceClient.getTrainerById(5L)).thenReturn(trainerWithFee);

            Booking savedBooking = buildSavedBooking("PAID", "PENDING");
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            BookingResponse response = bookingService.createBooking(sampleRequest);

            // Must default to PAID when plan-service is down (safe default)
            assertTrue(response.isPaymentRequired(), "Must default to PAID when plan-service is down");
        }

        @Test
        @DisplayName("Falls back to PAID when consume returns false (race condition)")
        void booking_consumeReturnsFalse_fallsBackToPaid() {
            // checkFreeSession says free but useIncludedSession says false (race)
            FreeSessionCheckResult freeCheck = new FreeSessionCheckResult(true, 1, 1, null);
            when(planServiceClient.checkFreeSession(10L)).thenReturn(freeCheck);
            when(planServiceClient.useIncludedSession(10L)).thenReturn(false);
            when(trainerServiceClient.getTrainerById(5L)).thenReturn(trainerWithFee);

            Booking savedBooking = buildSavedBooking("PAID", "PENDING");
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            BookingResponse response = bookingService.createBooking(sampleRequest);

            assertTrue(response.isPaymentRequired(), "Should fall back to PAID on race condition");
        }

        @Test
        @DisplayName("confirmPayment throws when booking is FREE_SESSION")
        void confirmPayment_onFreeBooking_throwsException() {
            Booking freeBooking = buildSavedBooking("FREE_SESSION", "COMPLETED");
            freeBooking.setId(1L);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(freeBooking));

            assertThrows(IllegalStateException.class, () -> bookingService.confirmPayment(1L),
                "Should not allow payment confirmation on a FREE_SESSION booking");
        }

        @Test
        @DisplayName("confirmPayment updates paymentStatus to COMPLETED")
        void confirmPayment_onPaidBooking_updatesStatus() {
            Booking paidBooking = buildSavedBooking("PAID", "PENDING");
            paidBooking.setId(2L);
            when(bookingRepository.findById(2L)).thenReturn(Optional.of(paidBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

            Booking result = bookingService.confirmPayment(2L);

            assertEquals("COMPLETED", result.getPaymentStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VALIDATION
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Request validation")
    class RequestValidation {

        @Test
        @DisplayName("Throws when memberId is null")
        void createBooking_nullMemberId_throws() {
            sampleRequest.setMemberId(null);
            assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(sampleRequest));
        }

        @Test
        @DisplayName("Throws when trainerId is null")
        void createBooking_nullTrainerId_throws() {
            sampleRequest.setTrainerId(null);
            assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(sampleRequest));
        }

        @Test
        @DisplayName("Throws when scheduleId is null")
        void createBooking_nullScheduleId_throws() {
            sampleRequest.setScheduleId(null);
            assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(sampleRequest));
        }

        @Test
        @DisplayName("Throws BookingNotFoundException for unknown booking ID")
        void getBookingById_unknown_throwsNotFound() {
            when(bookingRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(BookingNotFoundException.class, () -> bookingService.getBookingById(999L));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SESSION VALIDATION — calendar-based scheduling, capacity, duplicates
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Session validation — capacity, cancellation, expiry, duplicates")
    class SessionValidation {

        private TrainerScheduleDto schedule(LocalDate date, String start, String end, int cap, int booked, boolean cancelled) {
            TrainerScheduleDto dto = new TrainerScheduleDto();
            dto.setId(3L);
            dto.setTrainerId(5L);
            dto.setSessionDate(date);
            dto.setStartTime(start);
            dto.setEndTime(end);
            dto.setMaxCapacity(cap);
            dto.setBookedCount(booked);
            dto.setCancelled(cancelled);
            dto.setAvailable(!cancelled && booked < cap);
            return dto;
        }

        @Test
        @DisplayName("Rejects booking when session is already full")
        void createBooking_sessionFull_throws() {
            when(trainerServiceClient.getScheduleById(3L))
                .thenReturn(schedule(LocalDate.now().plusDays(1), "09:00", "11:00", 1, 1, false));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.createBooking(sampleRequest));
            assertTrue(ex.getMessage().toLowerCase().contains("full"));
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects booking when session has been cancelled by the trainer")
        void createBooking_sessionCancelled_throws() {
            when(trainerServiceClient.getScheduleById(3L))
                .thenReturn(schedule(LocalDate.now().plusDays(1), "09:00", "11:00", 20, 0, true));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.createBooking(sampleRequest));
            assertTrue(ex.getMessage().toLowerCase().contains("cancelled"));
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects booking when session has already expired")
        void createBooking_sessionExpired_throws() {
            when(trainerServiceClient.getScheduleById(3L))
                .thenReturn(schedule(LocalDate.now().minusDays(1), "09:00", "11:00", 20, 0, false));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.createBooking(sampleRequest));
            assertTrue(ex.getMessage().toLowerCase().contains("expired") || ex.getMessage().toLowerCase().contains("ended"));
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects duplicate booking — same member already has a CONFIRMED booking on this session")
        void createBooking_duplicateBooking_throws() {
            when(trainerServiceClient.getScheduleById(3L))
                .thenReturn(schedule(LocalDate.now().plusDays(1), "09:00", "11:00", 20, 0, false));
            when(bookingRepository.existsByMemberIdAndScheduleIdAndStatus(10L, 3L, "CONFIRMED"))
                .thenReturn(true);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.createBooking(sampleRequest));
            assertTrue(ex.getMessage().toLowerCase().contains("already booked"));
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects booking when scheduleId belongs to a different trainer")
        void createBooking_scheduleTrainerMismatch_throws() {
            TrainerScheduleDto mismatched = schedule(LocalDate.now().plusDays(1), "09:00", "11:00", 20, 0, false);
            mismatched.setTrainerId(999L);
            when(trainerServiceClient.getScheduleById(3L)).thenReturn(mismatched);

            assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(sampleRequest));
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Releases the reserved capacity slot when booking fails after reservation")
        void createBooking_failureAfterReservation_releasesCapacity() {
            when(trainerServiceClient.getScheduleById(3L))
                .thenReturn(schedule(LocalDate.now().plusDays(1), "09:00", "11:00", 20, 0, false));
            FreeSessionCheckResult paidCheck = new FreeSessionCheckResult(false, 0, 0, null);
            when(planServiceClient.checkFreeSession(10L)).thenReturn(paidCheck);
            TrainerDto trainerNoFee = new TrainerDto();
            trainerNoFee.setId(5L);
            trainerNoFee.setSessionFee(null);
            when(trainerServiceClient.getTrainerById(5L)).thenReturn(trainerNoFee);

            assertThrows(IllegalStateException.class, () -> bookingService.createBooking(sampleRequest));

            verify(trainerServiceClient).bookSlot(3L);
            verify(trainerServiceClient).unbookSlot(3L);
        }

        @Test
        @DisplayName("Successful booking reserves a capacity slot on the session")
        void createBooking_success_reservesCapacitySlot() {
            when(trainerServiceClient.getScheduleById(3L))
                .thenReturn(schedule(LocalDate.now().plusDays(1), "09:00", "11:00", 20, 0, false));
            FreeSessionCheckResult freeCheck = new FreeSessionCheckResult(true, 1, 1, null);
            when(planServiceClient.checkFreeSession(10L)).thenReturn(freeCheck);
            when(planServiceClient.useIncludedSession(10L)).thenReturn(true);
            Booking savedBooking = buildSavedBooking("FREE_SESSION", "COMPLETED");
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            bookingService.createBooking(sampleRequest);

            verify(trainerServiceClient).bookSlot(3L);
            verify(trainerServiceClient, never()).unbookSlot(anyLong());
        }

        @Test
        @DisplayName("Cancelling a booking releases the reserved capacity slot")
        void cancelBooking_releasesCapacitySlot() {
            Booking booking = buildSavedBooking("PAID", "PENDING");
            booking.setId(7L);
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

            bookingService.cancelBooking(7L);

            verify(trainerServiceClient).unbookSlot(3L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private Booking buildSavedBooking(String sessionType, String paymentStatus) {
        Booking b = new Booking();
        b.setId(1L);
        b.setMemberId(10L);
        b.setMemberEmail("member@gym.com");
        b.setTrainerId(5L);
        b.setTrainerEmail("trainer@gym.com");
        b.setScheduleId(3L);
        b.setSessionDay("MON");
        b.setSessionTime("09:00 - 11:00");
        b.setBookingDate(LocalDate.now());
        b.setStatus("CONFIRMED");
        b.setSessionType(sessionType);
        b.setPaymentStatus(paymentStatus);
        return b;
    }
}

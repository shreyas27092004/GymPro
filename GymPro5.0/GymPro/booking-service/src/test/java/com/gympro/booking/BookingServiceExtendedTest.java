package com.gympro.booking;

import com.gympro.booking.dto.BookingRequest;
import com.gympro.booking.dto.BookingResponse;
import com.gympro.booking.dto.FreeSessionCheckResult;
import com.gympro.booking.dto.NotificationEvent;
import com.gympro.booking.dto.TrainerDto;
import com.gympro.booking.dto.TrainerScheduleDto;
import com.gympro.booking.entity.Booking;
import com.gympro.booking.exception.BookingNotFoundException;
import com.gympro.booking.feign.AuthServiceClient;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Extended tests for {@link BookingService}, covering gaps not exercised by
 * {@link BookingServiceTest}: request-validation branches, get-by-member/trainer,
 * cancellation edge cases, payment-confirmation idempotency, completeBooking,
 * and admin-broadcast / notification-failure resilience.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService — extended coverage")
class BookingServiceExtendedTest {

    @Mock private BookingRepository     bookingRepository;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private PlanServiceClient     planServiceClient;
    @Mock private TrainerServiceClient  trainerServiceClient;
    @Mock private AuthServiceClient     authServiceClient;

    @InjectMocks
    private BookingService bookingService;

    private BookingRequest sampleRequest;
    private Booking sampleBooking;

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

        sampleBooking = new Booking();
        sampleBooking.setId(1L);
        sampleBooking.setMemberId(10L);
        sampleBooking.setMemberEmail("member@gym.com");
        sampleBooking.setTrainerId(5L);
        sampleBooking.setStatus("CONFIRMED");
        sampleBooking.setSessionType("PAID");
        sampleBooking.setPaymentStatus("PENDING");
        sampleBooking.setBookingDate(LocalDate.now());

        // ── Calendar-based session slot backing scheduleId=3 (trainer #5) ──
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

    // ── Request validation ──────────────────────────────────────────────

    @Test
    @DisplayName("createBooking throws when memberEmail is null")
    void createBooking_nullMemberEmail_throws() {
        sampleRequest.setMemberEmail(null);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(sampleRequest));
        verifyNoInteractions(bookingRepository);
    }

    // ── GET methods ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getBookingsByMember returns all bookings for the member")
    void getBookingsByMember_returnsList() {
        when(bookingRepository.findByMemberId(10L)).thenReturn(List.of(sampleBooking));

        List<Booking> result = bookingService.getBookingsByMember(10L);

        assertEquals(1, result.size());
        assertEquals(sampleBooking, result.get(0));
    }

    @Test
    @DisplayName("getBookingsByTrainer returns all bookings for the trainer")
    void getBookingsByTrainer_returnsList() {
        when(bookingRepository.findByTrainerId(5L)).thenReturn(List.of(sampleBooking));

        List<Booking> result = bookingService.getBookingsByTrainer(5L);

        assertEquals(1, result.size());
        assertEquals(sampleBooking, result.get(0));
    }

    // ── cancelBooking ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelBooking")
    class CancelBookingTests {

        @Test
        @DisplayName("Returns early without re-saving when already cancelled")
        void cancelBooking_alreadyCancelled_returnsMessageWithoutSaving() {
            sampleBooking.setStatus("CANCELLED");
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

            String result = bookingService.cancelBooking(1L);

            assertEquals("Booking already cancelled", result);
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Restores free session for a FREE_SESSION booking on cancel")
        void cancelBooking_freeSessionBooking_restoresSession() {
            sampleBooking.setSessionType("FREE_SESSION");
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(planServiceClient.restoreFreeSession(10L)).thenReturn("Session restored");

            bookingService.cancelBooking(1L);

            verify(planServiceClient, times(1)).restoreFreeSession(10L);
        }

        @Test
        @DisplayName("Cancellation still succeeds when restoring the free session throws")
        void cancelBooking_restoreFreeSessionThrows_stillSucceeds() {
            sampleBooking.setSessionType("FREE_SESSION");
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(planServiceClient.restoreFreeSession(10L)).thenThrow(new RuntimeException("plan-service down"));

            String result = bookingService.cancelBooking(1L);

            assertEquals("Booking cancelled ✅", result);
        }

        @Test
        @DisplayName("Does not attempt to restore a session for a PAID booking")
        void cancelBooking_paidBooking_doesNotRestoreSession() {
            sampleBooking.setSessionType("PAID");
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            bookingService.cancelBooking(1L);

            verify(planServiceClient, never()).restoreFreeSession(any());
        }

        @Test
        @DisplayName("Cancellation still succeeds even when the notification publish step throws")
        void cancelBooking_notificationPublishThrows_stillSucceeds() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceClient.getAdminUserIds()).thenReturn(List.of(1L));
            doThrow(new RuntimeException("mq down")).when(notificationPublisher).publishBookingEvent(any());

            String result = bookingService.cancelBooking(1L);

            assertEquals("Booking cancelled ✅", result);
        }

        @Test
        @DisplayName("Populates adminUserIds on the cancellation notification from AuthServiceClient")
        void cancelBooking_populatesAdminUserIds() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceClient.getAdminUserIds()).thenReturn(List.of(100L, 101L));

            bookingService.cancelBooking(1L);

            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notificationPublisher).publishBookingEvent(captor.capture());
            assertEquals(List.of(100L, 101L), captor.getValue().getAdminUserIds());
        }

        @Test
        @DisplayName("Falls back to an empty admin list when AuthServiceClient throws")
        void cancelBooking_authServiceThrows_fallsBackToEmptyAdminList() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceClient.getAdminUserIds()).thenThrow(new RuntimeException("auth-service down"));

            bookingService.cancelBooking(1L);

            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(notificationPublisher).publishBookingEvent(captor.capture());
            assertTrue(captor.getValue().getAdminUserIds().isEmpty());
        }

        @Test
        @DisplayName("Throws BookingNotFoundException for an unknown booking id")
        void cancelBooking_unknownId_throwsNotFound() {
            when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(BookingNotFoundException.class, () -> bookingService.cancelBooking(99L));
        }
    }

    // ── completeBooking ──────────────────────────────────────────────────

    @Test
    @DisplayName("completeBooking sets status to COMPLETED and saves when bookingDate is today")
    void completeBooking_todayDate_updatesStatusAndSaves() {
        sampleBooking.setBookingDate(LocalDate.now());
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        com.gympro.booking.dto.BookingCompletionResponse response = bookingService.completeBooking(1L);

        assertTrue(response.isSuccess());
        assertFalse(response.isConfirmationRequired());
        assertEquals("COMPLETED", response.getBooking().getStatus());
        assertEquals(1L, response.getBookingId());
        verify(bookingRepository, times(1)).save(sampleBooking);
    }

    @Test
    @DisplayName("completeBooking sets status to COMPLETED and saves when bookingDate is in the past")
    void completeBooking_pastDate_updatesStatusAndSaves() {
        sampleBooking.setBookingDate(LocalDate.now().minusDays(3));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        com.gympro.booking.dto.BookingCompletionResponse response = bookingService.completeBooking(1L);

        assertTrue(response.isSuccess());
        assertFalse(response.isConfirmationRequired());
        assertEquals("COMPLETED", response.getBooking().getStatus());
        verify(bookingRepository, times(1)).save(sampleBooking);
    }

    @Test
    @DisplayName("completeBooking on a FUTURE-dated booking returns confirmationRequired and does NOT save")
    void completeBooking_futureDate_noConfirm_returnsConfirmationRequired() {
        sampleBooking.setBookingDate(LocalDate.now().plusDays(5));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        com.gympro.booking.dto.BookingCompletionResponse response = bookingService.completeBooking(1L);

        assertFalse(response.isSuccess());
        assertTrue(response.isConfirmationRequired());
        assertEquals(1L, response.getBookingId());
        assertNotNull(response.getMessage());
        assertNull(response.getBooking());
        assertEquals("CONFIRMED", sampleBooking.getStatus()); // unchanged
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeBooking on a FUTURE-dated booking WITH confirm=true completes normally")
    void completeBooking_futureDate_withConfirm_completesBooking() {
        sampleBooking.setBookingDate(LocalDate.now().plusDays(5));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        com.gympro.booking.dto.BookingCompletionResponse response = bookingService.completeBooking(1L, true);

        assertTrue(response.isSuccess());
        assertFalse(response.isConfirmationRequired());
        assertEquals("COMPLETED", response.getBooking().getStatus());
        verify(bookingRepository, times(1)).save(sampleBooking);
    }

    @Test
    @DisplayName("completeBooking treats a null bookingDate as not-future and completes normally")
    void completeBooking_nullBookingDate_completesNormally() {
        sampleBooking.setBookingDate(null);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        com.gympro.booking.dto.BookingCompletionResponse response = bookingService.completeBooking(1L);

        assertTrue(response.isSuccess());
        assertFalse(response.isConfirmationRequired());
        verify(bookingRepository, times(1)).save(sampleBooking);
    }

    @Test
    @DisplayName("completeBooking throws BookingNotFoundException for an unknown id")
    void completeBooking_unknownId_throwsNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class, () -> bookingService.completeBooking(99L));
    }

    // ── confirmPayment ────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmPayment is idempotent — returns without re-saving when already COMPLETED")
    void confirmPayment_alreadyCompleted_returnsWithoutResaving() {
        sampleBooking.setSessionType("PAID");
        sampleBooking.setPaymentStatus("COMPLETED");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        Booking result = bookingService.confirmPayment(1L);

        assertEquals("COMPLETED", result.getPaymentStatus());
        verify(bookingRepository, never()).save(any());
    }

    // ── createBooking — notification resilience & admin broadcast ────────

    @Test
    @DisplayName("createBooking still returns a response even when notification publishing throws")
    void createBooking_notificationPublishThrows_stillSucceeds() {
        FreeSessionCheckResult noFree = new FreeSessionCheckResult(false, 0, 0, 1L);
        when(planServiceClient.checkFreeSession(10L)).thenReturn(noFree);
        TrainerDto trainer = new TrainerDto();
        trainer.setSessionFee(new BigDecimal("500.00"));
        when(trainerServiceClient.getTrainerById(5L)).thenReturn(trainer);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });
        doThrow(new RuntimeException("mq down")).when(notificationPublisher).publishBookingEvent(any());

        BookingResponse response = bookingService.createBooking(sampleRequest);

        assertTrue(response.isSuccess());
        assertTrue(response.isPaymentRequired());
    }

    @Test
    @DisplayName("createBooking populates adminUserIds on the confirmation notification")
    void createBooking_populatesAdminUserIds() {
        FreeSessionCheckResult noFree = new FreeSessionCheckResult(false, 0, 0, 1L);
        when(planServiceClient.checkFreeSession(10L)).thenReturn(noFree);
        TrainerDto trainer = new TrainerDto();
        trainer.setSessionFee(new BigDecimal("500.00"));
        when(trainerServiceClient.getTrainerById(5L)).thenReturn(trainer);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });
        when(authServiceClient.getAdminUserIds()).thenReturn(List.of(200L));

        bookingService.createBooking(sampleRequest);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationPublisher, atLeastOnce()).publishBookingEvent(captor.capture());
        boolean anyHasAdmins = captor.getAllValues().stream()
                .anyMatch(e -> e.getAdminUserIds() != null && e.getAdminUserIds().contains(200L));
        assertTrue(anyHasAdmins);
    }
}

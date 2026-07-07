package com.gympro.booking.exception;

import com.gympro.booking.dto.BookingRequest;
import com.gympro.booking.dto.BookingResponse;
import com.gympro.booking.entity.Booking;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ModelAndExceptionTest {

    // ── Booking entity ─────────────────────────────────────

    @Test
    void booking_DefaultConstructor_ShouldWork() {
        Booking booking = new Booking();
        assertNotNull(booking);
    }

    @Test
    void booking_AllArgsConstructor_ShouldWork() {
        LocalDate now = LocalDate.now();
        // Booking now has 13 fields: id, memberId, memberEmail, trainerId, trainerEmail,
        // scheduleId, sessionDay, sessionTime, bookingDate, status, notes, sessionType, paymentStatus
        Booking booking = new Booking(1L, 10L, "member@gym.com", 5L, "trainer@gym.com",
                3L, "MON", "09:00 - 11:00", now, "CONFIRMED", "Warm up first",
                "PAID", "PENDING");

        assertEquals(1L, booking.getId());
        assertEquals(10L, booking.getMemberId());
        assertEquals("member@gym.com", booking.getMemberEmail());
        assertEquals(5L, booking.getTrainerId());
        assertEquals("trainer@gym.com", booking.getTrainerEmail());
        assertEquals(3L, booking.getScheduleId());
        assertEquals("MON", booking.getSessionDay());
        assertEquals("09:00 - 11:00", booking.getSessionTime());
        assertEquals(now, booking.getBookingDate());
        assertEquals("CONFIRMED", booking.getStatus());
        assertEquals("Warm up first", booking.getNotes());
        assertEquals("PAID", booking.getSessionType());
        assertEquals("PENDING", booking.getPaymentStatus());
    }

    @Test
    void booking_SettersAndGetters_ShouldWork() {
        Booking booking = new Booking();
        LocalDate now = LocalDate.now();

        booking.setId(2L);
        booking.setMemberId(20L);
        booking.setMemberEmail("user@test.com");
        booking.setTrainerId(8L);
        booking.setTrainerEmail("coach@gym.com");
        booking.setScheduleId(7L);
        booking.setSessionDay("WED");
        booking.setSessionTime("14:00 - 16:00");
        booking.setBookingDate(now);
        booking.setStatus("CANCELLED");
        booking.setNotes("No notes");
        booking.setSessionType("FREE_SESSION");
        booking.setPaymentStatus("COMPLETED");

        assertEquals(2L, booking.getId());
        assertEquals(20L, booking.getMemberId());
        assertEquals("user@test.com", booking.getMemberEmail());
        assertEquals(8L, booking.getTrainerId());
        assertEquals("coach@gym.com", booking.getTrainerEmail());
        assertEquals(7L, booking.getScheduleId());
        assertEquals("WED", booking.getSessionDay());
        assertEquals("14:00 - 16:00", booking.getSessionTime());
        assertEquals(now, booking.getBookingDate());
        assertEquals("CANCELLED", booking.getStatus());
        assertEquals("No notes", booking.getNotes());
        assertEquals("FREE_SESSION", booking.getSessionType());
        assertEquals("COMPLETED", booking.getPaymentStatus());
    }

    @Test
    void booking_StatusTransitions_ShouldWork() {
        Booking booking = new Booking();
        booking.setStatus("CONFIRMED");
        assertEquals("CONFIRMED", booking.getStatus());

        booking.setStatus("CANCELLED");
        assertEquals("CANCELLED", booking.getStatus());

        booking.setStatus("COMPLETED");
        assertEquals("COMPLETED", booking.getStatus());
    }

    @Test
    void booking_SessionTypeValues_ShouldWork() {
        Booking booking = new Booking();
        booking.setSessionType("FREE_SESSION");
        assertEquals("FREE_SESSION", booking.getSessionType());

        booking.setSessionType("PAID");
        assertEquals("PAID", booking.getSessionType());
    }

    @Test
    void booking_PaymentStatusValues_ShouldWork() {
        Booking booking = new Booking();
        booking.setPaymentStatus("PENDING");
        assertEquals("PENDING", booking.getPaymentStatus());

        booking.setPaymentStatus("COMPLETED");
        assertEquals("COMPLETED", booking.getPaymentStatus());

        booking.setPaymentStatus("FAILED");
        assertEquals("FAILED", booking.getPaymentStatus());
    }

    @Test
    void booking_ToString_ShouldNotBeNull() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setMemberId(10L);
        assertNotNull(booking.toString());
    }

    @Test
    void booking_Equals_ShouldWork() {
        LocalDate now = LocalDate.now();
        Booking b1 = new Booking(1L, 10L, "m@g.com", 5L, "t@g.com", 3L, "MON", "09:00",
                now, "CONFIRMED", "notes", "PAID", "PENDING");
        Booking b2 = new Booking(1L, 10L, "m@g.com", 5L, "t@g.com", 3L, "MON", "09:00",
                now, "CONFIRMED", "notes", "PAID", "PENDING");
        assertEquals(b1, b2);
    }

    @Test
    void booking_HashCode_ShouldBeConsistent() {
        Booking booking = new Booking();
        booking.setId(1L);
        assertEquals(booking.hashCode(), booking.hashCode());
    }

    // ── BookingResponse DTO ────────────────────────────────

    @Test
    void bookingResponse_FreeSessionFactory_ShouldWork() {
        Booking booking = new Booking();
        booking.setId(1L);
        BookingResponse resp = BookingResponse.freeSession(booking, 3);

        assertTrue(resp.isSuccess());
        assertFalse(resp.isPaymentRequired());
        assertTrue(resp.isFreeSessionUsed());
        assertEquals(3, resp.getRemainingFreeSessions());
        assertNull(resp.getAmount());
        assertEquals(booking, resp.getBooking());
    }

    @Test
    void bookingResponse_PaymentRequiredFactory_ShouldWork() {
        Booking booking = new Booking();
        booking.setId(1L);
        BookingResponse resp = BookingResponse.paymentRequired(booking, new BigDecimal("500.00"));

        assertTrue(resp.isSuccess());
        assertTrue(resp.isPaymentRequired());
        assertFalse(resp.isFreeSessionUsed());
        assertEquals(0, resp.getRemainingFreeSessions());
        assertEquals(new BigDecimal("500.00"), resp.getAmount());
        assertEquals(booking, resp.getBooking());
    }

    // ── BookingRequest DTO ─────────────────────────────────

    @Test
    void bookingRequest_DefaultConstructor_ShouldWork() {
        BookingRequest req = new BookingRequest();
        assertNotNull(req);
    }

    @Test
    void bookingRequest_SettersAndGetters_ShouldWork() {
        BookingRequest req = new BookingRequest();
        req.setMemberId(10L);
        req.setMemberEmail("member@gym.com");
        req.setTrainerId(5L);
        req.setTrainerEmail("trainer@gym.com");
        req.setScheduleId(3L);
        req.setSessionDay("TUE");
        req.setSessionTime("11:00 - 13:00");
        req.setNotes("Focus on core");

        assertEquals(10L, req.getMemberId());
        assertEquals("member@gym.com", req.getMemberEmail());
        assertEquals(5L, req.getTrainerId());
        assertEquals("trainer@gym.com", req.getTrainerEmail());
        assertEquals(3L, req.getScheduleId());
        assertEquals("TUE", req.getSessionDay());
        assertEquals("11:00 - 13:00", req.getSessionTime());
        assertEquals("Focus on core", req.getNotes());
    }

    @Test
    void bookingRequest_ToString_ShouldNotBeNull() {
        BookingRequest req = new BookingRequest();
        req.setMemberId(1L);
        assertNotNull(req.toString());
    }

    // ── BookingNotFoundException ───────────────────────────

    @Test
    void bookingNotFoundException_ShouldContainId() {
        BookingNotFoundException ex = new BookingNotFoundException(15L);
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("15"));
    }

    @Test
    void bookingNotFoundException_ShouldHaveCorrectMessage() {
        BookingNotFoundException ex = new BookingNotFoundException(7L);
        assertEquals("Booking not found with ID: 7", ex.getMessage());
    }

    @Test
    void bookingNotFoundException_IsRuntimeException() {
        BookingNotFoundException ex = new BookingNotFoundException(1L);
        assertTrue(ex instanceof RuntimeException);
    }

    // ── AccessDeniedException ──────────────────────────────

    @Test
    void accessDeniedException_ShouldContainMessage() {
        AccessDeniedException ex = new AccessDeniedException("Only TRAINER or ADMIN");
        assertEquals("Only TRAINER or ADMIN", ex.getMessage());
    }

    @Test
    void accessDeniedException_IsRuntimeException() {
        AccessDeniedException ex = new AccessDeniedException("Denied");
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    void accessDeniedException_ShouldPreserveMessage() {
        String msg = "Access denied ❌ Only TRAINER or ADMIN can complete bookings";
        AccessDeniedException ex = new AccessDeniedException(msg);
        assertEquals(msg, ex.getMessage());
    }
}

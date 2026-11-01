package com.gympro.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gympro.booking.config.SecurityConfig;
import com.gympro.booking.dto.BookingRequest;
import com.gympro.booking.dto.BookingResponse;
import com.gympro.booking.entity.Booking;
import com.gympro.booking.exception.BookingNotFoundException;
import com.gympro.booking.exception.GlobalExceptionHandler;
import com.gympro.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
public class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Autowired
    private ObjectMapper objectMapper;

    private Booking sampleBooking;
    private BookingRequest sampleRequest;
    private BookingResponse freeSessionResponse;
    private BookingResponse paidResponse;

    @BeforeEach
    void setUp() {
        sampleBooking = new Booking();
        sampleBooking.setId(1L);
        sampleBooking.setMemberId(10L);
        sampleBooking.setMemberEmail("member@gym.com");
        sampleBooking.setTrainerId(5L);
        sampleBooking.setTrainerEmail("trainer@gym.com");
        sampleBooking.setScheduleId(3L);
        sampleBooking.setSessionDay("MON");
        sampleBooking.setSessionTime("09:00 - 11:00");
        sampleBooking.setBookingDate(LocalDate.now());
        sampleBooking.setStatus("CONFIRMED");
        sampleBooking.setSessionType("PAID");
        sampleBooking.setPaymentStatus("PENDING");
        sampleBooking.setNotes("Please warm up first");

        sampleRequest = new BookingRequest();
        sampleRequest.setMemberId(10L);
        sampleRequest.setMemberEmail("member@gym.com");
        sampleRequest.setTrainerId(5L);
        sampleRequest.setTrainerEmail("trainer@gym.com");
        sampleRequest.setScheduleId(3L);
        sampleRequest.setSessionDay("MON");
        sampleRequest.setSessionTime("09:00 - 11:00");
        sampleRequest.setNotes("Please warm up first");

        // BookingResponse for a free session
        freeSessionResponse = BookingResponse.freeSession(sampleBooking, 2);

        // BookingResponse for a paid session
        paidResponse = BookingResponse.paymentRequired(sampleBooking, new BigDecimal("500.00"));
    }

    // ── POST /bookings/create ──────────────────────────────

    @Test
    @DisplayName("POST /bookings/create - 200 with paymentRequired=false for free session")
    void createBooking_freeSession_returns200() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class))).thenReturn(freeSessionResponse);

        mockMvc.perform(post("/bookings/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paymentRequired").value(false))
                .andExpect(jsonPath("$.freeSessionUsed").value(true))
                .andExpect(jsonPath("$.remainingFreeSessions").value(2));
    }

    @Test
    @DisplayName("POST /bookings/create - 200 with paymentRequired=true for paid session")
    void createBooking_paidSession_returns200() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class))).thenReturn(paidResponse);

        mockMvc.perform(post("/bookings/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paymentRequired").value(true))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.freeSessionUsed").value(false));
    }

    @Test
    @DisplayName("POST /bookings/create - 400 when memberId is missing")
    void createBooking_missingMemberId_returns400() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class)))
            .thenThrow(new IllegalArgumentException("memberId is required"));

        mockMvc.perform(post("/bookings/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /bookings/create - 409 when session is full")
    void createBooking_sessionFull_returns409() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class)))
            .thenThrow(new IllegalStateException("This session is already full (capacity: 20)"));

        mockMvc.perform(post("/bookings/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /bookings/create - 409 when member already booked this session")
    void createBooking_duplicateBooking_returns409() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class)))
            .thenThrow(new IllegalStateException("You have already booked this session"));

        mockMvc.perform(post("/bookings/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isConflict());
    }

    // ── GET /bookings/{id} ─────────────────────────────────

    @Test
    @DisplayName("GET /bookings/1 - returns 200 with booking")
    void getById_ShouldReturnBooking_WhenExists() throws Exception {
        when(bookingService.getBookingById(1L)).thenReturn(sampleBooking);

        mockMvc.perform(get("/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /bookings/99 - returns 404 when not found")
    void getById_ShouldReturn404_WhenNotExists() throws Exception {
        when(bookingService.getBookingById(99L)).thenThrow(new BookingNotFoundException(99L));

        mockMvc.perform(get("/bookings/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── GET /bookings/member/{memberId} ────────────────────

    @Test
    @DisplayName("GET /bookings/member/10 - returns list of bookings")
    void byMember_ShouldReturnBookingsList() throws Exception {
        when(bookingService.getBookingsByMember(10L)).thenReturn(Arrays.asList(sampleBooking));

        mockMvc.perform(get("/bookings/member/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].memberId").value(10));
    }

    @Test
    @DisplayName("GET /bookings/member/99 - returns empty list")
    void byMember_ShouldReturnEmptyList_WhenNoBookings() throws Exception {
        when(bookingService.getBookingsByMember(99L)).thenReturn(List.of());

        mockMvc.perform(get("/bookings/member/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /bookings/trainer/{trainerId} ──────────────────

    @Test
    @DisplayName("GET /bookings/trainer/5 - returns list of bookings")
    void byTrainer_ShouldReturnBookingsList() throws Exception {
        when(bookingService.getBookingsByTrainer(5L)).thenReturn(Arrays.asList(sampleBooking));

        mockMvc.perform(get("/bookings/trainer/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].trainerId").value(5));
    }

    @Test
    @DisplayName("GET /bookings/trainer/99 - returns empty list")
    void byTrainer_ShouldReturnEmptyList_WhenNoBookings() throws Exception {
        when(bookingService.getBookingsByTrainer(99L)).thenReturn(List.of());

        mockMvc.perform(get("/bookings/trainer/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── POST /bookings/cancel/{id} ─────────────────────────

    @Test
    @DisplayName("POST /bookings/cancel/1 - returns success message")
    void cancelBooking_ShouldReturnSuccessMessage() throws Exception {
        when(bookingService.cancelBooking(1L)).thenReturn("Booking cancelled ✅");

        mockMvc.perform(post("/bookings/cancel/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Booking cancelled ✅"));
    }

    @Test
    @DisplayName("POST /bookings/cancel/99 - returns 404 when not found")
    void cancelBooking_ShouldReturn404_WhenNotExists() throws Exception {
        when(bookingService.cancelBooking(99L)).thenThrow(new BookingNotFoundException(99L));

        mockMvc.perform(post("/bookings/cancel/99"))
                .andExpect(status().isNotFound());
    }

    // ── POST /bookings/complete/{id} ───────────────────────

    @Test
    @DisplayName("POST /bookings/complete/1 - 200 + completed when TRAINER role and booking is not future-dated")
    void completeBooking_ShouldReturn200_WhenTrainerRole() throws Exception {
        sampleBooking.setStatus("COMPLETED");
        sampleBooking.setBookingDate(LocalDate.now());
        when(bookingService.completeBooking(1L, false))
                .thenReturn(com.gympro.booking.dto.BookingCompletionResponse.completed(sampleBooking));

        mockMvc.perform(post("/bookings/complete/1")
                .header("X-User-Role", "TRAINER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.confirmationRequired").value(false))
                .andExpect(jsonPath("$.booking.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /bookings/complete/1 - 200 + completed when ADMIN role and booking is not future-dated")
    void completeBooking_ShouldReturn200_WhenAdminRole() throws Exception {
        sampleBooking.setStatus("COMPLETED");
        sampleBooking.setBookingDate(LocalDate.now());
        when(bookingService.completeBooking(1L, false))
                .thenReturn(com.gympro.booking.dto.BookingCompletionResponse.completed(sampleBooking));

        mockMvc.perform(post("/bookings/complete/1")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.confirmationRequired").value(false))
                .andExpect(jsonPath("$.booking.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /bookings/complete/1 - 200 + confirmationRequired=true for a FUTURE booking with no confirm param")
    void completeBooking_futureBooking_noConfirm_returnsConfirmationRequired() throws Exception {
        when(bookingService.completeBooking(1L, false))
                .thenReturn(com.gympro.booking.dto.BookingCompletionResponse.confirmationRequired(
                        1L, "This booking is scheduled for a future date. Please confirm."));

        mockMvc.perform(post("/bookings/complete/1")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.confirmationRequired").value(true))
                .andExpect(jsonPath("$.bookingId").value(1))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.booking").doesNotExist());

        verify(bookingService, times(1)).completeBooking(1L, false);
    }

    @Test
    @DisplayName("POST /bookings/complete/1?confirm=true - 200 + completed for a FUTURE booking once confirmed")
    void completeBooking_futureBooking_withConfirm_completes() throws Exception {
        sampleBooking.setStatus("COMPLETED");
        sampleBooking.setBookingDate(LocalDate.now().plusDays(7));
        when(bookingService.completeBooking(1L, true))
                .thenReturn(com.gympro.booking.dto.BookingCompletionResponse.completed(sampleBooking));

        mockMvc.perform(post("/bookings/complete/1")
                .header("X-User-Role", "ADMIN")
                .param("confirm", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.confirmationRequired").value(false))
                .andExpect(jsonPath("$.booking.status").value("COMPLETED"));

        verify(bookingService, times(1)).completeBooking(1L, true);
    }

    @Test
    @DisplayName("POST /bookings/complete/1 - 403 when MEMBER role")
    void completeBooking_ShouldReturn403_WhenMemberRole() throws Exception {
        mockMvc.perform(post("/bookings/complete/1")
                .header("X-User-Role", "MEMBER"))
                .andExpect(status().isForbidden());

        verify(bookingService, never()).completeBooking(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("POST /bookings/complete/99 - 404 when not found")
    void completeBooking_ShouldReturn404_WhenNotExists() throws Exception {
        when(bookingService.completeBooking(99L, false)).thenThrow(new BookingNotFoundException(99L));

        mockMvc.perform(post("/bookings/complete/99")
                .header("X-User-Role", "TRAINER"))
                .andExpect(status().isNotFound());
    }

    // ── GET /bookings/test ─────────────────────────────────

    @Test
    @DisplayName("POST /bookings/1/confirm-payment - returns 200 with updated booking")
    void confirmPayment_ShouldReturn200_WithUpdatedBooking() throws Exception {
        Booking paidBooking = new Booking();
        paidBooking.setId(1L);
        paidBooking.setSessionType("PAID");
        paidBooking.setPaymentStatus("COMPLETED");
        when(bookingService.confirmPayment(1L)).thenReturn(paidBooking);

        mockMvc.perform(post("/bookings/1/confirm-payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /bookings/99/confirm-payment - returns 404 when booking not found")
    void confirmPayment_ShouldReturn404_WhenNotFound() throws Exception {
        when(bookingService.confirmPayment(99L)).thenThrow(new BookingNotFoundException(99L));

        mockMvc.perform(post("/bookings/99/confirm-payment"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /bookings/1/confirm-payment - returns 409 when booking is FREE_SESSION")
    void confirmPayment_ShouldReturn409_WhenFreeSession() throws Exception {
        when(bookingService.confirmPayment(1L))
                .thenThrow(new IllegalStateException("Booking #1 is a FREE_SESSION — no payment confirmation needed."));

        mockMvc.perform(post("/bookings/1/confirm-payment"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /bookings/test - returns health message")
    void testEndpoint_ShouldReturnWorkingMessage() throws Exception {
        mockMvc.perform(get("/bookings/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Booking Service Working ✅"));
    }
}

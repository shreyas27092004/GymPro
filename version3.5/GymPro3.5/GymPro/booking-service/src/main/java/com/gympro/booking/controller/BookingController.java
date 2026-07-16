package com.gympro.booking.controller;

import com.gympro.booking.dto.BookingRequest;
import com.gympro.booking.dto.BookingResponse;
import com.gympro.booking.entity.Booking;
import com.gympro.booking.exception.AccessDeniedException;
import com.gympro.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@Tag(name = "Bookings", description = "Create and manage trainer-session bookings")
public class BookingController {

    @Autowired
    private BookingService service;

    // ── Create ────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Create a booking",
        description = """
                      Books a training session. The backend decides FREE vs PAID automatically.
                      
                      Decision logic (backend-enforced, cannot be overridden by client):
                        - If member has active subscription with sessionsIncluded > sessionsUsed → FREE
                        - If member has sessionsIncluded=-1 (unlimited) → FREE
                        - If member has NO subscription and never used the 1 lifetime free session → FREE
                        - Otherwise → PAID (returns paymentRequired=true, amount=trainerFee)
                      
                      Frontend should initiate payment ONLY when paymentRequired=true.
                      Role: MEMBER.
                      """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking created",
            content = @Content(schema = @Schema(implementation = BookingResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or trainer fee not configured"),
        @ApiResponse(responseCode = "401", description = "Unauthorized – JWT required")
    })
    @PostMapping("/create")
    public BookingResponse create(@RequestBody BookingRequest req) {
        return service.createBooking(req);
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public Booking getById(@PathVariable Long id) {
        return service.getBookingById(id);
    }

    // ── Get by Member ─────────────────────────────────────────────────────────

    @GetMapping("/member/{memberId}")
    public List<Booking> byMember(@PathVariable Long memberId) {
        return service.getBookingsByMember(memberId);
    }

    // ── Get by Trainer ────────────────────────────────────────────────────────

    @GetMapping("/trainer/{trainerId}")
    public List<Booking> byTrainer(@PathVariable Long trainerId) {
        return service.getBookingsByTrainer(trainerId);
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Cancel a booking",
        description = "Cancels a booking. FREE_SESSION bookings automatically restore " +
                      "the consumed free session (works for both subscribed and non-subscribed members)."
    )
    @PostMapping("/cancel/{id}")
    public String cancel(@PathVariable Long id) {
        return service.cancelBooking(id);
    }

    // ── Complete ──────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Mark a booking as completed",
        description = "Transitions the booking to COMPLETED status. TRAINER or ADMIN only."
    )
    @PostMapping("/complete/{id}")
    public Booking complete(@PathVariable Long id,
                            @RequestHeader("X-User-Role") String role) {
        if (!"TRAINER".equals(role) && !"ADMIN".equals(role))
            throw new AccessDeniedException("Access denied ❌ Only TRAINER or ADMIN can complete bookings");
        return service.completeBooking(id);
    }

    // ── Confirm Payment ───────────────────────────────────────────────────────

    @Operation(
        summary     = "Mark a PAID booking's payment as completed",
        description = """
                      Called after a successful payment (Razorpay or cash) to mark
                      paymentStatus=COMPLETED on a PAID booking.
                      
                      Security note: this endpoint should ideally be called internally
                      by payment-service (not exposed to the public internet without
                      additional auth). Currently accepts MEMBER/ADMIN role for demo purposes.
                      A more secure approach is to call this from within payment-service
                      via an internal Feign call after Razorpay signature verification.
                      """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment confirmed, booking updated",
            content = @Content(schema = @Schema(implementation = Booking.class))),
        @ApiResponse(responseCode = "400", description = "Booking is FREE_SESSION (no payment needed)"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PostMapping("/{id}/confirm-payment")
    public Booking confirmPayment(@PathVariable Long id) {
        return service.confirmPayment(id);
    }

    // ── Health check ──────────────────────────────────────────────────────────

    @GetMapping("/test")
    public String test() { return "Booking Service Working ✅"; }
}

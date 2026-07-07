package com.gympro.payment.controller;

import com.gympro.payment.dto.PaymentRequest;
import com.gympro.payment.dto.RazorpayOrderResponse;
import com.gympro.payment.entity.Payment;
import com.gympro.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Razorpay payment flow: create orders, process payments, view history, and refunds")
public class PaymentController {

    @Autowired
    private PaymentService service;

    // ── Create Razorpay Order ──────────────────────────────────────────────────

    @Operation(
        summary     = "Create a Razorpay order",
        description = "Call this **before** showing the Razorpay payment widget on the frontend. "
                    + "Returns an `orderId` and `amount` (in paise) that the widget needs."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Razorpay order created",
            content = @Content(schema = @Schema(implementation = RazorpayOrderResponse.class))),
        @ApiResponse(responseCode = "500", description = "Razorpay API error")
    })
    @PostMapping("/create-order")
    public ResponseEntity<RazorpayOrderResponse> createOrder(
            @Parameter(description = "Payment amount in INR (₹)", example = "999.0", required = true)
            @RequestParam Double amount,
            @Parameter(description = "Short description shown in the Razorpay modal", example = "Booking #5")
            @RequestParam(defaultValue = "GymPro Payment") String description) {
        log.info("Creating Razorpay order: amount=₹{}", amount);
        return ResponseEntity.ok(service.createRazorpayOrder(amount, description));
    }

    // ── Process Payment ───────────────────────────────────────────────────────

    @Operation(
        summary     = "Process / record a payment",
        description = """
                      Handles two flows:
                      - **Razorpay**: supply `razorpayOrderId`, `razorpayPaymentId`, and `razorpaySignature` — the service verifies the signature before saving.
                      - **Dummy (UPI / CASH / QR)**: omit the Razorpay fields; the payment is recorded immediately.
                      """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment recorded",
            content = @Content(schema = @Schema(implementation = Payment.class))),
        @ApiResponse(responseCode = "400", description = "Razorpay signature mismatch or invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/pay")
    public ResponseEntity<Payment> pay(@RequestBody PaymentRequest req) {
        log.info("Payment request: member={}, amount=₹{}, method={}",
            req.getMemberEmail(), req.getAmount(), req.getPaymentMethod());
        return ResponseEntity.ok(service.processPayment(req));
    }

    // ── Get by Member ─────────────────────────────────────────────────────────

    @Operation(summary = "Get all payments for a member", description = "Role: MEMBER (own) or ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Payment.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/my/{memberId}")
    public ResponseEntity<List<Payment>> getMyPayments(
            @Parameter(description = "Member ID", example = "1") @PathVariable Long memberId) {
        return ResponseEntity.ok(service.getByMember(memberId));
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Operation(summary = "Get payment by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment found",
            content = @Content(schema = @Schema(implementation = Payment.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getById(
            @Parameter(description = "Payment ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // ── Get by Transaction ID ─────────────────────────────────────────────────

    @Operation(summary = "Get payment by transaction ID", description = "Looks up by the Razorpay / internal transaction ID.")
    @ApiResponse(responseCode = "200", description = "Payment found",
        content = @Content(schema = @Schema(implementation = Payment.class)))
    @GetMapping("/txn/{txnId}")
    public ResponseEntity<Payment> getByTxn(
            @Parameter(description = "Transaction / Razorpay payment ID", example = "pay_ABC123")
            @PathVariable String txnId) {
        return ResponseEntity.ok(service.getByTransactionId(txnId));
    }

    // ── Get by Booking ID ─────────────────────────────────────────────────────

    @Operation(summary = "Get payment linked to a booking")
    @ApiResponse(responseCode = "200", description = "Payment found",
        content = @Content(schema = @Schema(implementation = Payment.class)))
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<Payment> getByBooking(
            @Parameter(description = "Booking ID", example = "1") @PathVariable Long bookingId) {
        return ResponseEntity.ok(service.getByBookingId(bookingId));
    }

    // ── Get All (Admin) ───────────────────────────────────────────────────────

    @Operation(
        summary     = "Get all payments (Admin only)",
        description = "Returns the full payment history. Requires the `X-User-Role: ADMIN` header."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All payments",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Payment.class)))),
        @ApiResponse(responseCode = "403", description = "ADMIN role required")
    })
    @GetMapping("/all")
    public ResponseEntity<List<Payment>> getAll(
            @Parameter(description = "Caller role – must be ADMIN", example = "ADMIN", required = true)
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new SecurityException("ADMIN only");
        return ResponseEntity.ok(service.getAll());
    }

    // ── Refund ────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Refund a payment",
        description = "Issues a refund for the specified payment. Role: ADMIN."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refund processed",
            content = @Content(schema = @Schema(implementation = Payment.class))),
        @ApiResponse(responseCode = "403", description = "ADMIN role required"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PostMapping("/refund/{id}")
    public ResponseEntity<Payment> refund(
            @Parameter(description = "Payment ID to refund", example = "1") @PathVariable Long id,
            @Parameter(description = "Caller role – must be ADMIN", example = "ADMIN", required = true)
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(service.refund(id, role));
    }

    // ── Health check ──────────────────────────────────────────────────────────

    @Operation(summary = "Health check", description = "Liveness probe.")
    @ApiResponse(responseCode = "200", description = "Service is up")
    @GetMapping("/test")
    public String test() { return "Payment Service Working ✅ — Razorpay integrated"; }
}

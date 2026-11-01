package com.gympro.notification.controller;

import com.gympro.notification.dto.EmailRequest;
import com.gympro.notification.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ✅ REST controller for notification-service
// Called internally by other microservices via OpenFeign
// NOT exposed directly to end users (goes through gateway)
@Slf4j
@RestController
@RequestMapping("/notify")
public class NotificationController {

    @Autowired
    private EmailService emailService;

    // ─── GENERIC SEND (called by Feign clients) ────────────────────────
    // POST /notify/send?email=x&subject=y&message=z
    // This is the main endpoint Feign clients call
    @PostMapping("/send")
    public ResponseEntity<String> send(
            @RequestParam String email,
            @RequestParam String subject,
            @RequestParam String message) {

        log.info("📧 Notification request: to={}, subject={}", email, subject);
        emailService.sendEmail(email, subject, message);
        return ResponseEntity.ok("Email sent successfully to: " + email);
    }

    // ─── BOOKING EMAILS ────────────────────────────────────────────────

    // POST /notify/booking/member?to=x&bookingId=1&trainer=y&day=MON&time=09:00-11:00
    @PostMapping("/booking/member")
    public ResponseEntity<String> bookingMember(
            @RequestParam String to,
            @RequestParam Long bookingId,
            @RequestParam String trainerName,
            @RequestParam String day,
            @RequestParam String time) {

        emailService.sendBookingConfirmationToMember(to, bookingId, trainerName, day, time);
        return ResponseEntity.ok("Booking confirmation sent to member: " + to);
    }

    // POST /notify/booking/trainer
    @PostMapping("/booking/trainer")
    public ResponseEntity<String> bookingTrainer(
            @RequestParam String to,
            @RequestParam Long bookingId,
            @RequestParam String memberName,
            @RequestParam String day,
            @RequestParam String time) {

        emailService.sendBookingNotificationToTrainer(to, bookingId, memberName, day, time);
        return ResponseEntity.ok("Booking notification sent to trainer: " + to);
    }

    // POST /notify/booking/cancel?to=x&bookingId=1
    @PostMapping("/booking/cancel")
    public ResponseEntity<String> bookingCancel(
            @RequestParam String to,
            @RequestParam Long bookingId) {

        emailService.sendCancellationEmail(to, bookingId);
        return ResponseEntity.ok("Cancellation email sent to: " + to);
    }

    // ─── PAYMENT EMAILS ────────────────────────────────────────────────

    // POST /notify/payment/receipt
    @PostMapping("/payment/receipt")
    public ResponseEntity<String> paymentReceipt(
            @RequestParam String to,
            @RequestParam Double amount,
            @RequestParam String method,
            @RequestParam String txnId,
            @RequestParam String description) {

        emailService.sendPaymentReceipt(to, amount, method, txnId, description);
        return ResponseEntity.ok("Payment receipt sent to: " + to);
    }

    // POST /notify/payment/refund
    @PostMapping("/payment/refund")
    public ResponseEntity<String> paymentRefund(
            @RequestParam String to,
            @RequestParam Double amount,
            @RequestParam String txnId) {

        emailService.sendRefundEmail(to, amount, txnId);
        return ResponseEntity.ok("Refund email sent to: " + to);
    }

    // ─── PLAN EMAIL ────────────────────────────────────────────────────

    // POST /notify/plan/subscribed
    @PostMapping("/plan/subscribed")
    public ResponseEntity<String> planSubscribed(
            @RequestParam String to,
            @RequestParam String planName,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        emailService.sendPlanSubscriptionEmail(to, planName, startDate, endDate);
        return ResponseEntity.ok("Plan subscription email sent to: " + to);
    }

    // ─── OTP EMAIL ────────────────────────────────────────────────────────

    // POST /notify/otp — sends the 6-digit OTP for password reset
    @PostMapping("/otp")
    public ResponseEntity<String> sendOtp(
            @RequestParam String to,
            @RequestParam String otp) {

        emailService.sendOtpEmail(to, otp);
        return ResponseEntity.ok("OTP email sent to: " + to);
    }

    // ─── HEALTH CHECK ──────────────────────────────────────────────────
    @GetMapping("/test")
    public String test() {
        return "Notification Service Working ✅ — Gmail SMTP connected";
    }
}

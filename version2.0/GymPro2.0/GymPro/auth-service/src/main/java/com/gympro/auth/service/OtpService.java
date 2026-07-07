package com.gympro.auth.service;

import com.gympro.auth.dto.ForgotPasswordDtos.*;
import com.gympro.auth.dto.NotificationEvent;
import com.gympro.auth.entity.OtpEntry;
import com.gympro.auth.entity.User;
import com.gympro.auth.messaging.NotificationPublisher;
import com.gympro.auth.repository.OtpRepository;
import com.gympro.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

// ─────────────────────────────────────────────────────────────────────────────
//  OtpService – replaces synchronous Feign OTP email with async RabbitMQ event
//
//  Old flow:  OtpService → [Feign HTTP] → notification-service /notify/send
//  New flow:  OtpService → [RabbitMQ]   → gympro.otp.queue → notification-service
//
//  Key change: instead of building a long email body here and calling the
//  generic /notify/send endpoint, we publish a lean OTP_REQUESTED event.
//  notification-service renders the proper OTP email template via EmailService.
//
//  Old NotificationClient Feign kept as reference (commented out below).
// ─────────────────────────────────────────────────────────────────────────────
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_VALID_MINUTES = 10;

    @Autowired private OtpRepository        otpRepo;
    @Autowired private UserRepository       userRepo;
    @Autowired private PasswordEncoder      encoder;
    @Autowired private NotificationPublisher notificationPublisher;   // ← RabbitMQ publisher

    // ── OLD Feign injection (commented out — keep for fallback reference) ──
    // @Autowired private NotificationClient notificationClient;

    // ════════════════════════════════════════════════════════════════════════
    //  STEP 1: Generate OTP and send via RabbitMQ
    // ════════════════════════════════════════════════════════════════════════

    public ForgotPasswordResponse sendOtp(ForgotPasswordRequest req) {
        String email = req.getEmail();

        // Silently succeed even if email doesn't exist (don't leak user info)
        if (!userRepo.existsByEmail(email)) {
            log.warn("OTP requested for unknown email: {}", email);
            return new ForgotPasswordResponse(true,
                "If that email exists in our system, an OTP has been sent.");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        // Persist OTP (expires in 10 minutes, not yet used)
        OtpEntry entry = new OtpEntry();
        entry.setEmail(email);
        entry.setOtp(otp);
        entry.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES));
        entry.setUsed(false);
        otpRepo.save(entry);

        // ── Async OTP email via RabbitMQ ──────────────────────────────────
        try {
            notificationPublisher.publishOtpEvent(
                NotificationEvent.otpRequested(email, otp)
            );
            log.info("✅ OTP event published to RabbitMQ for: {}", email);
        } catch (Exception e) {
            // Publisher already logs AmqpException internally.
            // If RabbitMQ is completely unreachable, the OTP is still saved in DB —
            // user can retry and will get a fresh OTP attempt.
            log.error("❌ Failed to publish OTP event for {}: {}", email, e.getMessage());
            return new ForgotPasswordResponse(false,
                "Failed to send OTP email. Please try again.");
        }

        // ── OLD Feign approach (kept for reference) ───────────────────────
        // String subject = "GymPro – Password Reset OTP";
        // String message =
        //     "Hello!\n\n" +
        //     "You requested a password reset for your GymPro account.\n\n" +
        //     "Your OTP is:\n\n" +
        //     "        " + otp + "\n\n" +
        //     "This OTP is valid for " + OTP_VALID_MINUTES + " minutes.\n" +
        //     "Do NOT share this code with anyone.\n\n" +
        //     "If you did not request this, you can safely ignore this email.\n\n" +
        //     "GymPro Team 💪";
        // try {
        //     notificationClient.sendEmail(email, subject, message);
        //     log.info("✅ OTP sent to: {}", email);
        // } catch (Exception e) {
        //     log.error("❌ Failed to send OTP email to {}: {}", email, e.getMessage());
        //     return new ForgotPasswordResponse(false, "Failed to send OTP email. Please try again.");
        // }

        return new ForgotPasswordResponse(true,
            "OTP sent to your email. Valid for " + OTP_VALID_MINUTES + " minutes.");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  STEP 2: Verify OTP  (unchanged)
    // ════════════════════════════════════════════════════════════════════════

    public ForgotPasswordResponse verifyOtp(VerifyOtpRequest req) {
        OtpEntry entry = findValidOtp(req.getEmail(), req.getOtp());
        if (entry == null) {
            return new ForgotPasswordResponse(false, "Invalid or expired OTP.");
        }
        return new ForgotPasswordResponse(true, "OTP verified. You may now reset your password.");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  STEP 3: Reset Password  (unchanged)
    // ════════════════════════════════════════════════════════════════════════

    public ForgotPasswordResponse resetPassword(ResetPasswordRequest req) {
        OtpEntry entry = findValidOtp(req.getEmail(), req.getOtp());
        if (entry == null) {
            return new ForgotPasswordResponse(false, "Invalid or expired OTP.");
        }

        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            return new ForgotPasswordResponse(false, "Password must be at least 6 characters.");
        }

        User user = userRepo.findByEmail(req.getEmail()).orElse(null);
        if (user == null) {
            return new ForgotPasswordResponse(false, "User not found.");
        }
        user.setPassword(encoder.encode(req.getNewPassword()));
        userRepo.save(user);

        entry.setUsed(true);
        otpRepo.save(entry);

        log.info("✅ Password reset for: {}", req.getEmail());
        return new ForgotPasswordResponse(true, "Password reset successful! Please login.");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPER
    // ════════════════════════════════════════════════════════════════════════

    private OtpEntry findValidOtp(String email, String otp) {
        return otpRepo.findTopByEmailOrderByExpiresAtDesc(email)
            .filter(e -> !e.isUsed())
            .filter(e -> e.getExpiresAt().isAfter(LocalDateTime.now()))
            .filter(e -> e.getOtp().equals(otp))
            .orElse(null);
    }
}

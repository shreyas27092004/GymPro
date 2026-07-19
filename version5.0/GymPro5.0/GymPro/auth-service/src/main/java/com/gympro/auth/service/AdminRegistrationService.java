package com.gympro.auth.service;

import com.gympro.auth.dto.AdminRegistrationDtos.VerifyAdminRegistrationRequest;
import com.gympro.auth.dto.AuthResponse;
import com.gympro.auth.dto.NotificationEvent;
import com.gympro.auth.entity.AdminRegistrationRequest;
import com.gympro.auth.entity.User;
import com.gympro.auth.exception.EmailAlreadyExistsException;
import com.gympro.auth.messaging.NotificationPublisher;
import com.gympro.auth.repository.AdminRegistrationRepository;
import com.gympro.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

// ─────────────────────────────────────────────────────────────────────────────
//  AdminRegistrationService – "ask an existing admin" signup gate
//
//  Anyone can still fill in the register form with role=ADMIN, but the
//  account is NOT created immediately. Instead:
//
//    1. requestRegistration() — if this is the FIRST admin ever (bootstrap),
//       create the account directly, same as any other role. Otherwise,
//       generate a 6-digit code, stash the (already-hashed) signup details in
//       admin_registration_requests, and email that code to EVERY existing
//       admin. Nobody outside the app can complete the signup without one of
//       them handing the code over.
//
//    2. verifyRegistration() — the new admin submits their email + the code
//       an existing admin gave them. If it matches and hasn't expired/been
//       used, the User row is finally created with role=ADMIN.
// ─────────────────────────────────────────────────────────────────────────────
@Service
public class AdminRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(AdminRegistrationService.class);
    private static final int CODE_VALID_MINUTES = 10;

    @Autowired private UserRepository              userRepo;
    @Autowired private AdminRegistrationRepository pendingRepo;
    @Autowired private PasswordEncoder             encoder;
    @Autowired private NotificationPublisher       notificationPublisher;

    // ════════════════════════════════════════════════════════════════════════
    //  STEP 1: Request admin registration
    // ════════════════════════════════════════════════════════════════════════

    public AuthResponse requestRegistration(User user) {
        if (userRepo.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException(user.getEmail());
        }

        List<User> existingAdmins = userRepo.findByRole("ADMIN");

        // ── Bootstrap case: no admins exist yet — allow this one through directly ──
        if (existingAdmins.isEmpty()) {
            user.setRole("ADMIN");
            user.setPassword(encoder.encode(user.getPassword()));
            User saved = userRepo.save(user);
            log.info("[AdminRegistration] ✅ Bootstrap admin created (first admin, no approval needed): {}",
                saved.getEmail());
            return new AuthResponse(null, saved.getName(), saved.getEmail(), saved.getRole(),
                "Registered successfully ✅ You're the first admin, so no approval was needed. Please login.");
        }

        // ── Normal case: stash the pending signup and email a code to every admin ──
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        AdminRegistrationRequest pending = new AdminRegistrationRequest();
        pending.setName(user.getName());
        pending.setEmail(user.getEmail());
        pending.setPasswordHash(encoder.encode(user.getPassword()));
        pending.setOtp(otp);
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_VALID_MINUTES));
        pending.setUsed(false);
        pendingRepo.save(pending);

        int sent = 0;
        for (User admin : existingAdmins) {
            try {
                notificationPublisher.publishOtpEvent(
                    NotificationEvent.adminRegistrationCode(admin.getEmail(), otp, user.getName(), user.getEmail())
                );
                sent++;
            } catch (Exception e) {
                log.error("[AdminRegistration] ❌ Failed to publish approval-code event to {}: {}",
                    admin.getEmail(), e.getMessage());
            }
        }

        if (sent == 0) {
            return new AuthResponse(null, user.getName(), user.getEmail(), "ADMIN",
                "Failed to notify existing admins. Please try again.", false);
        }

        log.info("[AdminRegistration] 📨 Approval code sent to {} existing admin(s) for pending signup: {}",
            sent, user.getEmail());

        return new AuthResponse(null, user.getName(), user.getEmail(), "ADMIN",
            "Registration pending. An existing admin has been emailed a verification code — " +
            "ask them for it and enter it below to finish creating your admin account.",
            true);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  STEP 2: Verify the code and actually create the User
    // ════════════════════════════════════════════════════════════════════════

    public AuthResponse verifyRegistration(VerifyAdminRegistrationRequest req) {
        AdminRegistrationRequest pending = findValidRequest(req.getEmail(), req.getOtp());
        if (pending == null) {
            return new AuthResponse(null, null, req.getEmail(), "ADMIN",
                "Invalid or expired verification code. Ask an existing admin for a fresh one.", true);
        }

        if (userRepo.existsByEmail(pending.getEmail())) {
            // Extremely unlikely race — someone else registered this email in the meantime
            pending.setUsed(true);
            pendingRepo.save(pending);
            throw new EmailAlreadyExistsException(pending.getEmail());
        }

        User user = new User();
        user.setName(pending.getName());
        user.setEmail(pending.getEmail());
        user.setPassword(pending.getPasswordHash()); // already BCrypt-encoded
        user.setRole("ADMIN");
        User saved = userRepo.save(user);

        pending.setUsed(true);
        pendingRepo.save(pending);

        log.info("[AdminRegistration] ✅ Admin account approved and created: {}", saved.getEmail());
        return new AuthResponse(null, saved.getName(), saved.getEmail(), saved.getRole(),
            "Admin account verified and created ✅ Please login to get your token.");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPER
    // ════════════════════════════════════════════════════════════════════════

    private AdminRegistrationRequest findValidRequest(String email, String otp) {
        return pendingRepo.findTopByEmailOrderByExpiresAtDesc(email)
            .filter(r -> !r.isUsed())
            .filter(r -> r.getExpiresAt().isAfter(LocalDateTime.now()))
            .filter(r -> r.getOtp().equals(otp))
            .orElse(null);
    }
}

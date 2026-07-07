package com.gympro.auth.controller;

import com.gympro.auth.dto.AuthResponse;
import com.gympro.auth.dto.ForgotPasswordDtos.*;
import com.gympro.auth.dto.LoginRequest;
import com.gympro.auth.entity.User;
import com.gympro.auth.service.AuthService;
import com.gympro.auth.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Register, login, and OTP-based password reset")
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private OtpService  otpService;

    // ── Register ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Register a new user",
        description = "Creates a new user account. Returns the user's name, email, role, and a null token (login separately to obtain a JWT).",
        security    = {} // Public – no JWT required
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "409", description = "Email already registered",
            content = @Content(examples = @ExampleObject(value = "{\"message\":\"Email already exists\"}"))),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/register")
    public AuthResponse register(@RequestBody User user) {
        return authService.register(user);
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Login",
        description = "Authenticate with email + password. Returns a signed JWT in the `token` field. "
                    + "Use this token in the **Authorize** button above (paste without 'Bearer ' prefix).",
        security    = {} // Public – no JWT required
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful – JWT returned",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid email or password"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    // ── Forgot Password ──────────────────────────────────────────────────────

    @Operation(
        summary     = "Step 1 – Request OTP for password reset",
        description = "Generates a 6-digit OTP, saves it in the database (5-minute expiry), "
                    + "and emails it via the notification-service.",
        security    = {} // Public
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent to the registered email"),
        @ApiResponse(responseCode = "404", description = "No account found for the supplied email")
    })
    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@RequestBody ForgotPasswordRequest req) {
        return otpService.sendOtp(req);
    }

    // ── Verify OTP ───────────────────────────────────────────────────────────

    @Operation(
        summary     = "Step 2 – Verify OTP",
        description = "Validates that the 6-digit OTP matches and has not expired. "
                    + "Does NOT consume the OTP – you still need it in Step 3.",
        security    = {} // Public
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP is valid"),
        @ApiResponse(responseCode = "400", description = "OTP is invalid or expired")
    })
    @PostMapping("/verify-otp")
    public ForgotPasswordResponse verifyOtp(@RequestBody VerifyOtpRequest req) {
        return otpService.verifyOtp(req);
    }

    // ── Reset Password ───────────────────────────────────────────────────────

    @Operation(
        summary     = "Step 3 – Reset password",
        description = "Re-verifies the OTP, BCrypt-hashes the new password, updates the record, "
                    + "and marks the OTP as used so it cannot be replayed.",
        security    = {} // Public
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "OTP invalid, expired, or already used")
    })
    @PostMapping("/reset-password")
    public ForgotPasswordResponse resetPassword(@RequestBody ResetPasswordRequest req) {
        return otpService.resetPassword(req);
    }

    // ── Health check ─────────────────────────────────────────────────────────

    @Operation(summary = "Health check", description = "Liveness probe for the auth-service.", security = {})
    @ApiResponse(responseCode = "200", description = "Service is up")
    @GetMapping("/test")
    public String test() {
        return "Auth Service Working ✅";
    }

    // ── Internal: admin user IDs ─────────────────────────────────────────────
    // Called service-to-service (via Eureka, bypassing the Gateway) by
    // booking-service / payment-service so they know which user IDs to
    // broadcast admin in-app notifications to.

    @Operation(
        summary     = "Internal — list all ADMIN-role user IDs",
        description = "Used by other microservices to know which user IDs should receive "
                    + "broadcast admin in-app notifications. Not intended for frontend use.",
        security    = {}
    )
    @GetMapping("/internal/admin-ids")
    public java.util.List<Long> getAdminUserIds() {
        return authService.getAdminUserIds();
    }
}

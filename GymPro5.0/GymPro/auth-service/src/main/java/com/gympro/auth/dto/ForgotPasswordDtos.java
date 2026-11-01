package com.gympro.auth.dto;

import lombok.Data;

// ── Request: POST /auth/forgot-password ──────────────────────────────────
// Client sends just the email to request an OTP
public class ForgotPasswordDtos {

    @Data
    public static class ForgotPasswordRequest {
        private String email;
    }

    // ── Request: POST /auth/verify-otp ───────────────────────────────────
    // Client sends email + the 6-digit OTP they received
    @Data
    public static class VerifyOtpRequest {
        private String email;
        private String otp;
    }

    // ── Request: POST /auth/reset-password ───────────────────────────────
    // Client sends email + OTP (re-verified) + new password
    @Data
    public static class ResetPasswordRequest {
        private String email;
        private String otp;
        private String newPassword;
    }

    // ── Generic response for all three endpoints ─────────────────────────
    @Data
    public static class ForgotPasswordResponse {
        private boolean success;
        private String message;

        public ForgotPasswordResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}

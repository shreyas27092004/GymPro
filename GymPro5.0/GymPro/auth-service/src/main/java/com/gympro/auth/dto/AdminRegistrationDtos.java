package com.gympro.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ✅ Request body for POST /auth/verify-admin-registration
// The new admin submits the 6-digit code that an EXISTING admin received by
// email and read out to them — proves a real admin approved the signup.
public class AdminRegistrationDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyAdminRegistrationRequest {
        private String email;
        private String otp;
    }
}

package com.gympro.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// ✅ What we send back after login / register
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String name;
    private String email;
    private String role;
    private String message;

    // true when this was an ADMIN registration that is now PENDING —
    // the account has NOT been created yet; the frontend must show a
    // "enter the code an existing admin gave you" step next.
    private boolean verificationRequired;

    // Backward-compatible constructor for all the existing call sites
    // (normal register/login — never require extra verification).
    public AuthResponse(String token, String name, String email, String role, String message) {
        this(token, name, email, role, message, false);
    }
}

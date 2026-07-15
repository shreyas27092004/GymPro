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
}

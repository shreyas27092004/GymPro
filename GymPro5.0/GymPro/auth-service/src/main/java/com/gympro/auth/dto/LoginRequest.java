package com.gympro.auth.dto;

import lombok.Data;

// ✅ What the client sends when logging in
@Data
public class LoginRequest {
    private String email;
    private String password;
}

package com.gympro.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// ✅ Stores OTP codes temporarily for password reset
// Each row is one OTP attempt — expired or used entries are ignored
@Entity
@Table(name = "otp_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String otp;              // 6-digit code

    @Column(nullable = false)
    private LocalDateTime expiresAt; // valid for 10 minutes

    @Column(nullable = false)
    private boolean used;            // true once successfully verified
}

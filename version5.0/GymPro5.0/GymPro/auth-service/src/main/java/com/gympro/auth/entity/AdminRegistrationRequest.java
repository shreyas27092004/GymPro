package com.gympro.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// ✅ Holds a pending ADMIN registration until an existing admin's approval
// code is entered by the person trying to register.
//
// No row in the `users` table is created until verifyRegistration() succeeds —
// this table is the only place the new admin's (already-hashed) password lives
// in the meantime.
@Entity
@Table(name = "admin_registration_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRegistrationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;   // already BCrypt-encoded — copied verbatim onto the User once verified

    @Column(nullable = false)
    private String otp;            // 6-digit code, sent to every existing admin's email

    @Column(nullable = false)
    private LocalDateTime expiresAt; // valid for 10 minutes

    @Column(nullable = false)
    private boolean used;          // true once successfully verified and the User was created
}

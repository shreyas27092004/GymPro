package com.gympro.auth.entity;

import jakarta.persistence.*;
import lombok.*;

// ✅ Represents a user in the system
// Roles: MEMBER, TRAINER, ADMIN
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;   // stored as BCrypt hash

    @Column(nullable = false)
    private String role;       // MEMBER | TRAINER | ADMIN
}

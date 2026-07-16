package com.gympro.trainer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

// ✅ Trainer profile – ADMIN creates/manages trainers
@Entity
@Table(name = "trainers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;
    private String specialization;   // e.g. "Weight Loss", "Yoga", "Cardio"
    private int experienceYears;
    private String status;           // ACTIVE | INACTIVE

    // ── NEW: Fee charged per training session ─────────────────────────────
    // Stored in rupees (e.g. 500.00). NULL = fee not yet configured.
    @DecimalMin(value = "0.0", inclusive = false, message = "Session fee must be greater than 0")
    @Column(name = "session_fee", precision = 10, scale = 2)
    private BigDecimal sessionFee;
}

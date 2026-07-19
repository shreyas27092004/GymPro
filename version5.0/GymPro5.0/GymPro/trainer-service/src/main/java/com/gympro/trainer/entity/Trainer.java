package com.gympro.trainer.entity;

import com.gympro.trainer.validation.IndianMobileNumber;
import jakarta.persistence.*;
import lombok.*;

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

    @IndianMobileNumber
    @Column(unique = true)
    private String phone;
    private String specialization;   // e.g. "Weight Loss", "Yoga", "Cardio"
    private int experienceYears;
    // PENDING  = just self-registered, awaiting admin approval (cannot log in / not shown to members)
    // ACTIVE   = approved by admin, fully functional
    // INACTIVE = deactivated by admin after being active
    // REJECTED = admin rejected the approval request
    private String status;

    // ── NEW: Fee charged per training session ─────────────────────────────
    // Stored in rupees (e.g. 500.00). NULL = fee not yet configured.
    @DecimalMin(value = "0.0", inclusive = false, message = "Session fee must be greater than 0")
    @Column(name = "session_fee", precision = 10, scale = 2)
    private BigDecimal sessionFee;
}
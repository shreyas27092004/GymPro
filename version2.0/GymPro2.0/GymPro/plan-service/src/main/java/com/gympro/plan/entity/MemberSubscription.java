package com.gympro.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "member_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;
    private String memberEmail;

    private Long planId;
    private String planName;

    private LocalDate startDate;
    private LocalDate endDate;
    private String status;          // ACTIVE | EXPIRED | CANCELLED

    // Tracks how many included sessions have been used
    private int sessionsUsed;
}
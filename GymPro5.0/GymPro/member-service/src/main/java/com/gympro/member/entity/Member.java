package com.gympro.member.entity;

import com.gympro.member.validation.IndianMobileNumber;
import jakarta.persistence.*;
import lombok.*;

// ✅ Member profile – linked to a user account (by email)
@Entity
@Table(name = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;         // same email as auth user

    @IndianMobileNumber
    @Column(unique = true)
    private String phone;
    private String address;
    private String gender;

    private String status;        // ACTIVE | INACTIVE
}
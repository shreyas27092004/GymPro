package com.gympro.auth.repository;

import com.gympro.auth.entity.OtpEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntry, Long> {

    // Find the most recent unused, unexpired OTP for an email
    // (ordered by expiresAt DESC so the latest is first)
    Optional<OtpEntry> findTopByEmailOrderByExpiresAtDesc(String email);
}

package com.gympro.auth.repository;

import com.gympro.auth.entity.AdminRegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminRegistrationRepository extends JpaRepository<AdminRegistrationRequest, Long> {

    // Find the most recent unused, unexpired admin registration request for an email
    // (ordered by expiresAt DESC so the latest attempt is checked first)
    Optional<AdminRegistrationRequest> findTopByEmailOrderByExpiresAtDesc(String email);
}

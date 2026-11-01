package com.gympro.auth.service;

import com.gympro.auth.dto.AuthResponse;
import com.gympro.auth.dto.LoginRequest;
import com.gympro.auth.entity.User;
import com.gympro.auth.exception.EmailAlreadyExistsException;
import com.gympro.auth.exception.InvalidCredentialsException;
import com.gympro.auth.exception.TrainerNotApprovedException;
import com.gympro.auth.exception.UserNotFoundException;
import com.gympro.auth.feign.TrainerServiceClient;
import com.gympro.auth.repository.UserRepository;
import com.gympro.auth.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired private UserRepository       repo;
    @Autowired private JwtUtil              jwtUtil;
    @Autowired private PasswordEncoder      encoder;
    @Autowired private TrainerServiceClient trainerServiceClient;

    /** All user IDs with role=ADMIN — used to broadcast admin in-app notifications. */
    public List<Long> getAdminUserIds() {
        return repo.findByRole("ADMIN").stream()
            .map(User::getId)
            .collect(Collectors.toList());
    }

    public AuthResponse register(User user) {
        if (repo.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException(user.getEmail());
        }
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("MEMBER");
        }
        user.setPassword(encoder.encode(user.getPassword()));
        User saved = repo.save(user);

        if ("TRAINER".equalsIgnoreCase(saved.getRole())) {
            try {
                Map<String, Object> trainerPayload = new HashMap<>();
                trainerPayload.put("name",            saved.getName());
                trainerPayload.put("email",           saved.getEmail());
                trainerPayload.put("phone",           "");
                trainerPayload.put("specialization",  "General");
                trainerPayload.put("experienceYears", 0);
                // Self-registered trainers start PENDING — an admin must approve
                // them (see TrainerController#approve) before they can log in.
                trainerPayload.put("status",          "PENDING");

                trainerServiceClient.createTrainer(trainerPayload, "ADMIN");
                log.info("[AuthService] ✅ Trainer profile auto-created (PENDING approval) for: {}", saved.getEmail());
            } catch (Exception e) {
                log.warn("[AuthService] ⚠️ Could not auto-create trainer profile for {}: {}",
                    saved.getEmail(), e.getMessage());
            }

            return new AuthResponse(null, saved.getName(), saved.getEmail(), saved.getRole(),
                "Registered successfully ✅ Your trainer account is pending admin approval. " +
                "You'll be able to log in once an admin approves it.");
        }

        return new AuthResponse(null, saved.getName(), saved.getEmail(), saved.getRole(),
            "Registered successfully ✅ Please login to get your token.");
    }

    public AuthResponse login(LoginRequest req) {
        User user = repo.findByEmail(req.getEmail())
            .orElseThrow(() -> new UserNotFoundException(req.getEmail()));

        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // Trainers must be approved (status=ACTIVE in trainer-service) before
        // they're allowed to log in — blocks self-registered trainers who are
        // still PENDING, and any that were REJECTED/deactivated by an admin.
        if ("TRAINER".equalsIgnoreCase(user.getRole())) {
            String status;
            try {
                Map<String, Object> trainer = trainerServiceClient.getByEmail(user.getEmail());
                status = trainer != null ? String.valueOf(trainer.get("status")) : null;
            } catch (Exception e) {
                log.warn("[AuthService] ⚠️ Could not verify trainer approval status for {}: {}",
                    user.getEmail(), e.getMessage());
                throw TrainerNotApprovedException.forStatus(null);
            }
            if (!"ACTIVE".equalsIgnoreCase(status)) {
                throw TrainerNotApprovedException.forStatus(status);
            }
        }

        // Passes userId so the JWT carries the "userId" claim —
        // used by the frontend for notification API calls and SSE stream URL
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getId());
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole(),
            "Login successful ✅");
    }
}
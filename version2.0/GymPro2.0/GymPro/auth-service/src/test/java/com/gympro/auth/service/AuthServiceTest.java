package com.gympro.auth.service;

import com.gympro.auth.dto.AuthResponse;
import com.gympro.auth.dto.LoginRequest;
import com.gympro.auth.entity.User;
import com.gympro.auth.exception.EmailAlreadyExistsException;
import com.gympro.auth.exception.InvalidCredentialsException;
import com.gympro.auth.exception.UserNotFoundException;
import com.gympro.auth.repository.UserRepository;
import com.gympro.auth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}.
 * All dependencies are mocked – no database or Spring context required.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // ----------------------------------------------------------------
    // Mocked dependencies
    // ----------------------------------------------------------------
    @Mock
    private UserRepository repo;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder encoder;

    // System under test – Mockito injects the mocks above automatically
    @InjectMocks
    private AuthService authService;

    // ----------------------------------------------------------------
    // Shared test data
    // ----------------------------------------------------------------
    private User sampleUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setName("Shreyas");
        sampleUser.setEmail("shreyas@gympro.com");
        sampleUser.setPassword("rawPassword");
        sampleUser.setRole("MEMBER");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("shreyas@gympro.com");
        loginRequest.setPassword("rawPassword");
    }

    // ================================================================
    // register() tests
    // ================================================================

    @Test
    @DisplayName("register - success: new user is saved and no token is returned (login required)")
    void register_success() {
        // Arrange
        when(repo.existsByEmail(sampleUser.getEmail())).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("hashedPassword");
        when(repo.save(any(User.class))).thenReturn(sampleUser);

        // Act
        AuthResponse response = authService.register(sampleUser);

        // Assert
        assertNotNull(response);
        assertNull(response.getToken()); // register does NOT return a token
        assertEquals("shreyas@gympro.com", response.getEmail());
        assertEquals("MEMBER", response.getRole());
        assertTrue(response.getMessage().contains("Registered"));

        // Verify interactions
        verify(repo, times(1)).existsByEmail(sampleUser.getEmail());
        verify(encoder, times(1)).encode(anyString());
        verify(repo, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("register - TRAINER role: attempts auto-create of trainer profile (may silently fail)")
    void register_trainerRole_attemptsAutoCreate() {
        // Arrange
        sampleUser.setRole("TRAINER");
        User savedTrainer = new User(2L, "Shreyas", "shreyas@gympro.com", "hashedPassword", "TRAINER");
        when(repo.existsByEmail(sampleUser.getEmail())).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("hashedPassword");
        when(repo.save(any(User.class))).thenReturn(savedTrainer);

        // Act — trainer-service is not running, so the HTTP call will fail, but must not throw
        AuthResponse response = authService.register(sampleUser);

        // Assert
        assertNotNull(response);
        assertEquals("TRAINER", response.getRole());
        assertTrue(response.getMessage().contains("Registered"));
    }

    @Test
    @DisplayName("register - success: null role defaults to MEMBER")
    void register_defaultsRoleToMember_whenRoleIsNull() {
        // Arrange
        sampleUser.setRole(null);
        when(repo.existsByEmail(anyString())).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("hashed");
        when(repo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            // After save, role should have been defaulted
            assertEquals("MEMBER", u.getRole());
            return u;
        });

        // Act
        AuthResponse response = authService.register(sampleUser);

        // Assert
        assertNotNull(response);
        assertEquals("MEMBER", response.getRole());
    }

    @Test
    @DisplayName("register - success: blank role defaults to MEMBER")
    void register_defaultsRoleToMember_whenRoleIsBlank() {
        sampleUser.setRole("   ");
        when(repo.existsByEmail(anyString())).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("hashed");
        when(repo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            assertEquals("MEMBER", u.getRole());
            return u;
        });

        AuthResponse response = authService.register(sampleUser);
        assertNotNull(response);
        assertEquals("MEMBER", response.getRole());
    }

    @Test
    @DisplayName("register - failure: throws EmailAlreadyExistsException for duplicate email")
    void register_throwsEmailAlreadyExists_whenEmailTaken() {
        // Arrange
        when(repo.existsByEmail(sampleUser.getEmail())).thenReturn(true);

        // Act & Assert
        EmailAlreadyExistsException ex = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(sampleUser)
        );
        assertTrue(ex.getMessage().contains("shreyas@gympro.com"));

        // Ensure save was never called
        verify(repo, never()).save(any(User.class));
    }

    // ================================================================
    // login() tests
    // ================================================================

    @Test
    @DisplayName("login - success: correct credentials return a token")
    void login_success() {
        // Arrange – password stored as hash, encoder.matches returns true
        sampleUser.setPassword("hashedPassword");
        when(repo.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(sampleUser));
        when(encoder.matches("rawPassword", "hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(sampleUser.getEmail(), sampleUser.getRole()))
                .thenReturn("mocked-jwt-token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.getToken());
        assertEquals("shreyas@gympro.com", response.getEmail());
        assertTrue(response.getMessage().contains("Login successful"));

        verify(encoder, times(1)).matches("rawPassword", "hashedPassword");
        verify(jwtUtil, times(1)).generateToken(sampleUser.getEmail(), sampleUser.getRole());
    }

    @Test
    @DisplayName("login - failure: throws UserNotFoundException for unknown email")
    void login_throwsUserNotFound_whenEmailDoesNotExist() {
        // Arrange
        when(repo.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> authService.login(loginRequest)
        );
        assertTrue(ex.getMessage().contains("shreyas@gympro.com"));

        // Password check and token generation must NOT happen
        verify(encoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("login - failure: throws InvalidCredentialsException for wrong password")
    void login_throwsInvalidCredentials_whenPasswordWrong() {
        // Arrange
        sampleUser.setPassword("hashedPassword");
        when(repo.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(sampleUser));
        when(encoder.matches("rawPassword", "hashedPassword")).thenReturn(false);

        // Act & Assert
        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }
}

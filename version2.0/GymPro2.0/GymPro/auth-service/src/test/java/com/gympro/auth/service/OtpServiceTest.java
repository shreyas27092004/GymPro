package com.gympro.auth.service;

import com.gympro.auth.dto.ForgotPasswordDtos.ForgotPasswordRequest;
import com.gympro.auth.dto.ForgotPasswordDtos.ForgotPasswordResponse;
import com.gympro.auth.dto.ForgotPasswordDtos.ResetPasswordRequest;
import com.gympro.auth.dto.ForgotPasswordDtos.VerifyOtpRequest;
import com.gympro.auth.dto.NotificationEvent;
import com.gympro.auth.entity.OtpEntry;
import com.gympro.auth.entity.User;
import com.gympro.auth.messaging.NotificationPublisher;
import com.gympro.auth.repository.OtpRepository;
import com.gympro.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OtpService}.
 * All external dependencies are mocked — no Spring context, no DB, no SMTP.
 * OtpService now publishes OTP events via RabbitMQ (NotificationPublisher),
 * not via the old Feign NotificationClient.
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock private OtpRepository otpRepo;
    @Mock private UserRepository userRepo;
    @Mock private PasswordEncoder encoder;
    @Mock private NotificationPublisher notificationPublisher;

    @InjectMocks
    private OtpService otpService;

    private static final String EMAIL = "user@gympro.com";

    // ================================================================
    // sendOtp() tests
    // ================================================================

    @Test
    @DisplayName("sendOtp - unknown email returns generic success (security: no info leak)")
    void sendOtp_unknownEmail_returnsGenericSuccess() {
        when(userRepo.existsByEmail(EMAIL)).thenReturn(false);

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail(EMAIL);

        ForgotPasswordResponse resp = otpService.sendOtp(req);

        assertTrue(resp.isSuccess());
        assertNotNull(resp.getMessage());
        // Ensure no OTP was saved or event published
        verify(otpRepo, never()).save(any());
        verify(notificationPublisher, never()).publishOtpEvent(any());
    }

    @Test
    @DisplayName("sendOtp - known email generates OTP, saves it, and publishes RabbitMQ event")
    void sendOtp_knownEmail_savesOtpAndPublishesEvent() {
        when(userRepo.existsByEmail(EMAIL)).thenReturn(true);
        when(otpRepo.save(any(OtpEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(notificationPublisher).publishOtpEvent(any(NotificationEvent.class));

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail(EMAIL);

        ForgotPasswordResponse resp = otpService.sendOtp(req);

        assertTrue(resp.isSuccess());

        // OTP was saved with correct fields
        ArgumentCaptor<OtpEntry> captor = ArgumentCaptor.forClass(OtpEntry.class);
        verify(otpRepo).save(captor.capture());
        OtpEntry saved = captor.getValue();
        assertEquals(EMAIL, saved.getEmail());
        assertFalse(saved.isUsed());
        assertNotNull(saved.getOtp());
        assertEquals(6, saved.getOtp().length());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));

        // RabbitMQ event was published
        verify(notificationPublisher).publishOtpEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("sendOtp - RabbitMQ publish failure returns failure response")
    void sendOtp_publishFails_returnsFalse() {
        when(userRepo.existsByEmail(EMAIL)).thenReturn(true);
        when(otpRepo.save(any(OtpEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("RabbitMQ unreachable"))
            .when(notificationPublisher).publishOtpEvent(any(NotificationEvent.class));

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail(EMAIL);

        ForgotPasswordResponse resp = otpService.sendOtp(req);

        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().toLowerCase().contains("failed") ||
                   resp.getMessage().toLowerCase().contains("try again"));
    }

    // ================================================================
    // verifyOtp() tests
    // ================================================================

    @Test
    @DisplayName("verifyOtp - valid OTP returns success")
    void verifyOtp_validOtp_returnsSuccess() {
        OtpEntry entry = buildValidOtpEntry("123456");
        when(otpRepo.findTopByEmailOrderByExpiresAtDesc(EMAIL))
            .thenReturn(Optional.of(entry));

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail(EMAIL);
        req.setOtp("123456");

        ForgotPasswordResponse resp = otpService.verifyOtp(req);

        assertTrue(resp.isSuccess());
    }

    @Test
    @DisplayName("verifyOtp - wrong OTP returns failure")
    void verifyOtp_wrongOtp_returnsFailure() {
        OtpEntry entry = buildValidOtpEntry("111111");
        when(otpRepo.findTopByEmailOrderByExpiresAtDesc(EMAIL))
            .thenReturn(Optional.of(entry));

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail(EMAIL);
        req.setOtp("999999");

        ForgotPasswordResponse resp = otpService.verifyOtp(req);

        assertFalse(resp.isSuccess());
    }

    @Test
    @DisplayName("verifyOtp - expired OTP returns failure")
    void verifyOtp_expiredOtp_returnsFailure() {
        OtpEntry entry = new OtpEntry();
        entry.setEmail(EMAIL);
        entry.setOtp("123456");
        entry.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        entry.setUsed(false);

        when(otpRepo.findTopByEmailOrderByExpiresAtDesc(EMAIL))
            .thenReturn(Optional.of(entry));

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail(EMAIL);
        req.setOtp("123456");

        ForgotPasswordResponse resp = otpService.verifyOtp(req);

        assertFalse(resp.isSuccess());
    }

    @Test
    @DisplayName("verifyOtp - already used OTP returns failure")
    void verifyOtp_usedOtp_returnsFailure() {
        OtpEntry entry = buildValidOtpEntry("123456");
        entry.setUsed(true);

        when(otpRepo.findTopByEmailOrderByExpiresAtDesc(EMAIL))
            .thenReturn(Optional.of(entry));

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail(EMAIL);
        req.setOtp("123456");

        ForgotPasswordResponse resp = otpService.verifyOtp(req);

        assertFalse(resp.isSuccess());
    }

    @Test
    @DisplayName("verifyOtp - no OTP record exists returns failure")
    void verifyOtp_noRecord_returnsFailure() {
        when(otpRepo.findTopByEmailOrderByExpiresAtDesc(EMAIL))
            .thenReturn(Optional.empty());

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail(EMAIL);
        req.setOtp("123456");

        ForgotPasswordResponse resp = otpService.verifyOtp(req);

        assertFalse(resp.isSuccess());
    }

    // ================================================================
    // resetPassword() tests
    // ================================================================

    @Test
    @DisplayName("resetPassword - valid OTP and password resets successfully")
    void resetPassword_valid_resetsPassword() {
        OtpEntry entry = buildValidOtpEntry("654321");
        User user = new User(1L, "Test", EMAIL, "oldHash", "MEMBER");

        when(otpRepo.findTopByEmailOrderByExpiresAtDesc(EMAIL))
            .thenReturn(Optional.of(entry));
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(encoder.encode("newPass123")).thenReturn("newHash");
        when(userRepo.save(any(User.class))).thenReturn(user);
        when(otpRepo.save(any(OtpEntry.class))).thenReturn(entry);

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail(EMAIL);
        req.setOtp("654321");
        req.setNewPassword("newPass123");

        ForgotPasswordResponse resp = otpService.resetPassword(req);

        assertTrue(resp.isSuccess());
        assertTrue(entry.isUsed());
        verify(encoder).encode("newPass123");
        verify(userRepo).save(user);
        verify(otpRepo).save(entry);
    }

    @Test
    @DisplayName("resetPassword - invalid OTP returns failure")
    void resetPassword_invalidOtp_returnsFailure() {
        when(otpRepo.findTopByEmailOrderByExpiresAtDesc(EMAIL))
            .thenReturn(Optional.empty());

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail(EMAIL);
        req.setOtp("000000");
        req.setNewPassword("newPass123");

        ForgotPasswordResponse resp = otpService.resetPassword(req);

        assertFalse(resp.isSuccess());
        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("resetPassword - too-short password returns failure")
    void resetPassword_shortPassword_returnsFailure() {
        OtpEntry entry = buildValidOtpEntry("123456");
        when(otpRepo.findTopByEmailOrderByExpiresAtDesc(EMAIL))
            .thenReturn(Optional.of(entry));

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail(EMAIL);
        req.setOtp("123456");
        req.setNewPassword("abc");

        ForgotPasswordResponse resp = otpService.resetPassword(req);

        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().contains("6"));
    }

    @Test
    @DisplayName("resetPassword - null password returns failure")
    void resetPassword_nullPassword_returnsFailure() {
        OtpEntry entry = buildValidOtpEntry("123456");
        when(otpRepo.findTopByEmailOrderByExpiresAtDesc(EMAIL))
            .thenReturn(Optional.of(entry));

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail(EMAIL);
        req.setOtp("123456");
        req.setNewPassword(null);

        ForgotPasswordResponse resp = otpService.resetPassword(req);

        assertFalse(resp.isSuccess());
    }

    @Test
    @DisplayName("resetPassword - user not found returns failure")
    void resetPassword_userNotFound_returnsFailure() {
        OtpEntry entry = buildValidOtpEntry("123456");
        when(otpRepo.findTopByEmailOrderByExpiresAtDesc(EMAIL))
            .thenReturn(Optional.of(entry));
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.empty());

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail(EMAIL);
        req.setOtp("123456");
        req.setNewPassword("validPassword");

        ForgotPasswordResponse resp = otpService.resetPassword(req);

        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().toLowerCase().contains("not found") ||
                   resp.getMessage().toLowerCase().contains("user"));
    }

    // ================================================================
    // Helper
    // ================================================================

    private OtpEntry buildValidOtpEntry(String otp) {
        OtpEntry entry = new OtpEntry();
        entry.setId(1L);
        entry.setEmail(EMAIL);
        entry.setOtp(otp);
        entry.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        entry.setUsed(false);
        return entry;
    }
}

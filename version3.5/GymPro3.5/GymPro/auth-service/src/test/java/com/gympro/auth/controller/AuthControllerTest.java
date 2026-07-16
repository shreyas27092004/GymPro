package com.gympro.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gympro.auth.dto.AuthResponse;
import com.gympro.auth.dto.ForgotPasswordDtos.ForgotPasswordRequest;
import com.gympro.auth.dto.ForgotPasswordDtos.ForgotPasswordResponse;
import com.gympro.auth.dto.ForgotPasswordDtos.ResetPasswordRequest;
import com.gympro.auth.dto.ForgotPasswordDtos.VerifyOtpRequest;
import com.gympro.auth.dto.LoginRequest;
import com.gympro.auth.entity.User;
import com.gympro.auth.exception.EmailAlreadyExistsException;
import com.gympro.auth.exception.GlobalExceptionHandler;
import com.gympro.auth.exception.InvalidCredentialsException;
import com.gympro.auth.exception.UserNotFoundException;
import com.gympro.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private com.gympro.auth.service.OtpService otpService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
       
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /auth/test - returns 200 with health message")
    void test_endpoint_returnsOk() throws Exception {
        mockMvc.perform(get("/auth/test"))
               .andExpect(status().isOk())
               .andExpect(content().string(org.hamcrest.Matchers.containsString("Auth Service Working")));
    }

    
    @Test
    @DisplayName("POST /auth/register - 200 on successful registration")
    void register_success_returns200() throws Exception {
        // Arrange
        User user = new User();
        user.setName("Shreyas");
        user.setEmail("shreyas@gympro.com");
        user.setPassword("pass123");
        user.setRole("MEMBER");

        AuthResponse mockResponse = new AuthResponse("jwt-token", null, "shreyas@gympro.com", "MEMBER", "Registered successfully ✅");
        when(authService.register(any(User.class))).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.token").value("jwt-token"))
               .andExpect(jsonPath("$.email").value("shreyas@gympro.com"))
               .andExpect(jsonPath("$.role").value("MEMBER"))
               .andExpect(jsonPath("$.message").value("Registered successfully ✅"));

        verify(authService, times(1)).register(any(User.class));
    }

    @Test
    @DisplayName("POST /auth/register - 409 when email already exists")
    void register_duplicateEmail_returns409() throws Exception {
        // Arrange
        User user = new User();
        user.setEmail("existing@gympro.com");
        user.setPassword("pass");
        user.setRole("MEMBER");

        when(authService.register(any(User.class)))
                .thenThrow(new EmailAlreadyExistsException("existing@gympro.com"));

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
               .andExpect(status().isConflict())
               .andExpect(jsonPath("$.status").value(409))
               .andExpect(jsonPath("$.error").value("Conflict"));
    }

   
    @Test
    @DisplayName("POST /auth/login - 200 on successful login")
    void login_success_returns200() throws Exception {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setEmail("shreyas@gympro.com");
        req.setPassword("pass123");

        AuthResponse mockResponse = new AuthResponse("jwt-token", null, "shreyas@gympro.com", "MEMBER", "Login successful ✅");
        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.token").value("jwt-token"))
               .andExpect(jsonPath("$.message").value("Login successful ✅"));
    }

    @Test
    @DisplayName("POST /auth/login - 404 when user not found")
    void login_userNotFound_returns404() throws Exception {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@gympro.com");
        req.setPassword("pass");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UserNotFoundException("unknown@gympro.com"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$.status").value(404))
               .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("POST /auth/login - 401 when password is wrong")
    void login_wrongPassword_returns401() throws Exception {
        // Arrange
        LoginRequest req = new LoginRequest();
        req.setEmail("shreyas@gympro.com");
        req.setPassword("wrongPass");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.status").value(401))
               .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    // ================================================================
    // OTP endpoint tests
    // ================================================================

    @Test
    @DisplayName("POST /auth/forgot-password - 200 on valid email")
    void forgotPassword_returns200() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("user@gympro.com");

        ForgotPasswordResponse mockResp = new ForgotPasswordResponse(true, "OTP sent");
        when(otpService.sendOtp(any(ForgotPasswordRequest.class))).thenReturn(mockResp);

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /auth/verify-otp - 200 on valid OTP")
    void verifyOtp_returns200() throws Exception {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail("user@gympro.com");
        req.setOtp("123456");

        ForgotPasswordResponse mockResp = new ForgotPasswordResponse(true, "OTP verified");
        when(otpService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(mockResp);

        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /auth/reset-password - 200 on valid reset")
    void resetPassword_returns200() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("user@gympro.com");
        req.setOtp("123456");
        req.setNewPassword("newPass123");

        ForgotPasswordResponse mockResp = new ForgotPasswordResponse(true, "Password reset successful");
        when(otpService.resetPassword(any(ResetPasswordRequest.class))).thenReturn(mockResp);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true));
    }
}


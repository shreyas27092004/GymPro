package com.gympro.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gympro.member.entity.Member;
import com.gympro.member.exception.AccessDeniedException;
import com.gympro.member.exception.DuplicatePhoneException;
import com.gympro.member.exception.MemberNotFoundException;
import com.gympro.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.gympro.member.config.SecurityConfig;

@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class)
public class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    @Autowired
    private ObjectMapper objectMapper;

    private Member sampleMember;

    @BeforeEach
    void setUp() {
        sampleMember = new Member();
        sampleMember.setId(1L);
        sampleMember.setName("John Doe");
        sampleMember.setEmail("john@example.com");
        sampleMember.setPhone("9876543210");
        sampleMember.setAddress("123 Main St");
        sampleMember.setGender("Male");
        sampleMember.setStatus("ACTIVE");
    }

    // ─── POST /members ────────────────────────────────────────────────────────

    @Test
    void createMember_ShouldReturn200WithCreatedMember() throws Exception {
        when(memberService.createMember(any(Member.class))).thenReturn(sampleMember);

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleMember)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(memberService, times(1)).createMember(any(Member.class));
    }

    // ─── POST /members – phone validation ───────────────────────────────────

    @Test
    void createMember_WithPhoneNotStartingWith6To9_ShouldReturn400() throws Exception {
        sampleMember.setPhone("5876543210");

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleMember)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").value("Phone number must start with 6, 7, 8, or 9"));

        verify(memberService, never()).createMember(any());
    }

    @Test
    void createMember_WithTooShortPhone_ShouldReturn400() throws Exception {
        sampleMember.setPhone("12345");

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleMember)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone")
                        .value("Phone number must be exactly 10 digits (do not include a country code, spaces, or symbols)"));

        verify(memberService, never()).createMember(any());
    }

    @Test
    void createMember_WithAlphabeticPhone_ShouldReturn400() throws Exception {
        sampleMember.setPhone("abcdefghij");

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleMember)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone")
                        .value("Phone number must contain digits only (letters and special characters are not allowed)"));

        verify(memberService, never()).createMember(any());
    }

    @Test
    void createMember_WithBlankPhone_ShouldReturn400() throws Exception {
        sampleMember.setPhone("");

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleMember)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").value("Phone number must not be blank"));

        verify(memberService, never()).createMember(any());
    }

    @Test
    void createMember_WithDuplicatePhone_ShouldReturn409() throws Exception {
        when(memberService.createMember(any(Member.class)))
                .thenThrow(new DuplicatePhoneException("9876543210"));

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleMember)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Phone number 9876543210 is already registered with another member"));
    }

    @Test
    void updateMember_WithInvalidPhone_ShouldReturn400() throws Exception {
        sampleMember.setPhone("99999"); // wrong length

        mockMvc.perform(put("/members/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleMember)))
                .andExpect(status().isBadRequest());

        verify(memberService, never()).updateMember(any(), any());
    }

    // ─── GET /members ─────────────────────────────────────────────────────────

    @Test
    void getAllMembers_WithAdminRole_ShouldReturn200() throws Exception {
        List<Member> members = Arrays.asList(sampleMember);
        when(memberService.getAllMembers()).thenReturn(members);

        mockMvc.perform(get("/members")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("John Doe"));

        verify(memberService, times(1)).getAllMembers();
    }

    @Test
    void getAllMembers_WithNonAdminRole_ShouldThrowAccessDenied() throws Exception {
        // Controller throws AccessDeniedException before calling service when role != ADMIN
        mockMvc.perform(get("/members")
                        .header("X-User-Role", "MEMBER"))
                .andExpect(status().isForbidden());

        verify(memberService, never()).getAllMembers();
    }

    // ─── GET /members/{id} ────────────────────────────────────────────────────

    @Test
    void getMemberById_ShouldReturn200WhenFound() throws Exception {
        when(memberService.getMemberById(1L)).thenReturn(sampleMember);

        mockMvc.perform(get("/members/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void getMemberById_ShouldReturn404WhenNotFound() throws Exception {
        when(memberService.getMemberById(99L)).thenThrow(new MemberNotFoundException(99L));

        mockMvc.perform(get("/members/99"))
                .andExpect(status().isNotFound());
    }

    // ─── PUT /members/{id} ────────────────────────────────────────────────────

    @Test
    void updateMember_ShouldReturn200WithUpdatedMember() throws Exception {
        Member updated = new Member();
        updated.setName("John Updated");
        updated.setPhone("8888888888");
        updated.setAddress("456 New St");
        updated.setGender("Male");

        Member savedMember = new Member();
        savedMember.setId(1L);
        savedMember.setName("John Updated");
        savedMember.setEmail("john@example.com");
        savedMember.setPhone("8888888888");
        savedMember.setAddress("456 New St");
        savedMember.setGender("Male");
        savedMember.setStatus("ACTIVE");

        when(memberService.updateMember(eq(1L), any(Member.class))).thenReturn(savedMember);

        mockMvc.perform(put("/members/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.phone").value("8888888888"));
    }

    @Test
    void updateMember_ShouldReturn404WhenMemberNotFound() throws Exception {
        when(memberService.updateMember(eq(99L), any(Member.class)))
                .thenThrow(new MemberNotFoundException(99L));

        mockMvc.perform(put("/members/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleMember)))
                .andExpect(status().isNotFound());
    }

    // ─── DELETE /members/{id} ─────────────────────────────────────────────────

    @Test
    void deleteMember_WithAdminRole_ShouldReturn200() throws Exception {
        when(memberService.deleteMember(1L)).thenReturn("Member deactivated ✅");

        mockMvc.perform(delete("/members/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());

        verify(memberService, times(1)).deleteMember(1L);
    }

    @Test
    void deleteMember_WithNonAdminRole_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/members/1")
                        .header("X-User-Role", "MEMBER"))
                .andExpect(status().isForbidden());

        verify(memberService, never()).deleteMember(any());
    }

    // ─── GET /members/by-email/{email} ────────────────────────────────────────

    @Test
    void getMemberByEmail_ShouldReturn200WhenFound() throws Exception {
        when(memberService.getMemberByEmail("john@example.com")).thenReturn(sampleMember);

        mockMvc.perform(get("/members/by-email/john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(memberService, times(1)).getMemberByEmail("john@example.com");
    }

    @Test
    void getMemberByEmail_ShouldReturn404WhenNotFound() throws Exception {
        when(memberService.getMemberByEmail("missing@example.com"))
                .thenThrow(new MemberNotFoundException("Member not found with email: missing@example.com"));

        mockMvc.perform(get("/members/by-email/missing@example.com"))
                .andExpect(status().isNotFound());
    }

    // ─── GET /members/test ────────────────────────────────────────────────────

    @Test
    void testEndpoint_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/members/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Member Service Working ✅"));
    }
}

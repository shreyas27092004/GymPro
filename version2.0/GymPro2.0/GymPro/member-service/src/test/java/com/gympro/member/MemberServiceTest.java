package com.gympro.member;

import com.gympro.member.entity.Member;
import com.gympro.member.exception.MemberNotFoundException;
import com.gympro.member.repository.MemberRepository;
import com.gympro.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

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

    // ─── createMember ────────────────────────────────────────────────────────

    @Test
    void createMember_ShouldSetStatusActiveAndSave() {
        Member input = new Member();
        input.setEmail("jane@example.com");
        input.setName("Jane");

        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        Member result = memberService.createMember(input);

        assertEquals("ACTIVE", result.getStatus());
        verify(memberRepository, times(1)).save(input);
    }

    @Test
    void createMember_ShouldThrowWhenEmailIsBlank() {
        Member input = new Member();
        input.setEmail("  ");
        input.setName("No Email");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberService.createMember(input));

        assertTrue(ex.getMessage().contains("email"));
        verify(memberRepository, never()).save(any());
    }

    @Test
    void createMember_ShouldThrowWhenEmailIsNull() {
        Member input = new Member();
        input.setEmail(null);

        assertThrows(IllegalArgumentException.class,
                () -> memberService.createMember(input));
    }

    // ─── getAllMembers ────────────────────────────────────────────────────────

    @Test
    void getAllMembers_ShouldReturnList() {
        when(memberRepository.findAll()).thenReturn(Arrays.asList(sampleMember));

        List<Member> result = memberService.getAllMembers();

        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
        verify(memberRepository, times(1)).findAll();
    }

    // ─── getMemberById ───────────────────────────────────────────────────────

    @Test
    void getMemberById_ShouldReturnMemberWhenFound() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));

        Member result = memberService.getMemberById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void getMemberById_ShouldThrowMemberNotFoundExceptionWhenNotFound() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        MemberNotFoundException ex = assertThrows(MemberNotFoundException.class,
                () -> memberService.getMemberById(99L));

        assertTrue(ex.getMessage().contains("99"));
    }

    // ─── updateMember ────────────────────────────────────────────────────────

    @Test
    void updateMember_ShouldUpdateFieldsAndSave() {
        Member updated = new Member();
        updated.setName("John Updated");
        updated.setPhone("1111111111");
        updated.setAddress("456 New St");
        updated.setGender("Male");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        Member result = memberService.updateMember(1L, updated);

        assertEquals("John Updated", result.getName());
        assertEquals("1111111111", result.getPhone());
        assertEquals("456 New St", result.getAddress());
        verify(memberRepository, times(1)).save(sampleMember);
    }

    @Test
    void updateMember_ShouldThrowWhenMemberNotFound() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(MemberNotFoundException.class,
                () -> memberService.updateMember(99L, new Member()));
    }

    // ─── deleteMember ────────────────────────────────────────────────────────

    @Test
    void deleteMember_ShouldSetStatusInactiveAndReturnMessage() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = memberService.deleteMember(1L);

        assertEquals("INACTIVE", sampleMember.getStatus());
        assertTrue(result.contains("deactivated"));
        verify(memberRepository, times(1)).save(sampleMember);
    }

    // ─── getMemberByEmail ────────────────────────────────────────────────────

    @Test
    void getMemberByEmail_ShouldReturnMemberWhenFound() {
        when(memberRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleMember));

        Member result = memberService.getMemberByEmail("john@example.com");

        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
        verify(memberRepository, times(1)).findByEmail("john@example.com");
    }

    @Test
    void getMemberByEmail_ShouldThrowMemberNotFoundExceptionWhenNotFound() {
        when(memberRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(MemberNotFoundException.class,
                () -> memberService.getMemberByEmail("missing@example.com"));
    }

    // ─── updateMember – status branch ────────────────────────────────────────

    @Test
    void updateMember_ShouldOverwriteStatusWhenProvidedAndNonBlank() {
        Member updated = new Member();
        updated.setName("John Updated");
        updated.setStatus("INACTIVE");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        Member result = memberService.updateMember(1L, updated);

        assertEquals("INACTIVE", result.getStatus());
    }

    @Test
    void updateMember_ShouldKeepExistingStatusWhenUpdatedStatusIsBlank() {
        Member updated = new Member();
        updated.setName("John Updated");
        updated.setStatus("   ");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        Member result = memberService.updateMember(1L, updated);

        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    void updateMember_ShouldKeepExistingStatusWhenUpdatedStatusIsNull() {
        Member updated = new Member();
        updated.setName("John Updated");
        updated.setStatus(null);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        Member result = memberService.updateMember(1L, updated);

        assertEquals("ACTIVE", result.getStatus());
    }
}

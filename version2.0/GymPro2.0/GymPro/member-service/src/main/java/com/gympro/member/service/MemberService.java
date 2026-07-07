package com.gympro.member.service;

import com.gympro.member.entity.Member;
import com.gympro.member.exception.MemberNotFoundException;
import com.gympro.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    @Autowired
    private MemberRepository repo;

    public Member createMember(Member member) {
        if (member.getEmail() == null || member.getEmail().isBlank()) {
            throw new IllegalArgumentException("Member email must not be blank");
        }
        member.setStatus("ACTIVE");
        return repo.save(member);
    }

    public List<Member> getAllMembers() {
        return repo.findAll();
    }

    public Member getMemberById(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new MemberNotFoundException(id));
    }

    public Member getMemberByEmail(String email) {
        return repo.findByEmail(email)
            .orElseThrow(() -> new MemberNotFoundException(0L));
    }

    public Member updateMember(Long id, Member updated) {
        Member existing = getMemberById(id);
        existing.setName(updated.getName());
        existing.setPhone(updated.getPhone());
        existing.setAddress(updated.getAddress());
        existing.setGender(updated.getGender());
        // Persist status changes (ACTIVE / INACTIVE)
        if (updated.getStatus() != null && !updated.getStatus().isBlank()) {
            existing.setStatus(updated.getStatus());
        }
        return repo.save(existing);
    }

    public String deleteMember(Long id) {
        Member member = getMemberById(id);
        member.setStatus("INACTIVE");
        repo.save(member);
        return "Member deactivated ✅";
    }
}
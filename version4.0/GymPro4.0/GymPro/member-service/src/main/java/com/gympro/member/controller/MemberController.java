package com.gympro.member.controller;

import com.gympro.member.entity.Member;
import com.gympro.member.exception.AccessDeniedException;
import com.gympro.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members")
@Tag(name = "Members", description = "Gym member registration and profile management")
public class MemberController {

    @Autowired
    private MemberService service;

    // ── Create ────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Register a new member",
        description = "Creates a member profile. Typically called after auth-service registration. Role: ADMIN or self."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Member created",
            content = @Content(schema = @Schema(implementation = Member.class))),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public Member create(@RequestBody Member member) {
        return service.createMember(member);
    }

    // ── Get All ───────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Get all members (Admin only)",
        description = "Returns every member record. Requires `X-User-Role: ADMIN` header."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Member list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Member.class)))),
        @ApiResponse(responseCode = "403", description = "Access denied – ADMIN only")
    })
    @GetMapping
    public List<Member> getAll(
            @Parameter(description = "Caller role – must be ADMIN", example = "ADMIN", required = true)
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new AccessDeniedException("Access denied ❌ ADMIN only");
        return service.getAllMembers();
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Operation(summary = "Get member by ID", description = "Role: MEMBER (own) or ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Member found",
            content = @Content(schema = @Schema(implementation = Member.class))),
        @ApiResponse(responseCode = "404", description = "Member not found")
    })
    @GetMapping("/{id}")
    public Member getById(
            @Parameter(description = "Member ID", example = "1") @PathVariable Long id) {
        return service.getMemberById(id);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Operation(summary = "Update member profile", description = "Role: MEMBER (own) or ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Member updated",
            content = @Content(schema = @Schema(implementation = Member.class))),
        @ApiResponse(responseCode = "404", description = "Member not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{id}")
    public Member update(
            @Parameter(description = "Member ID", example = "1") @PathVariable Long id,
            @RequestBody Member member) {
        return service.updateMember(id, member);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Delete a member (Admin only)",
        description = "Permanently removes the member record. Requires `X-User-Role: ADMIN` header."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Member deleted – confirmation message"),
        @ApiResponse(responseCode = "403", description = "Access denied – ADMIN only"),
        @ApiResponse(responseCode = "404", description = "Member not found")
    })
    @DeleteMapping("/{id}")
    public String delete(
            @Parameter(description = "Member ID to delete", example = "1") @PathVariable Long id,
            @Parameter(description = "Caller role – must be ADMIN", example = "ADMIN", required = true)
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new AccessDeniedException("Access denied ❌ ADMIN only");
        return service.deleteMember(id);
    }

    // ── Get by Email ──────────────────────────────────────────────────────────

    @Operation(summary = "Get member by email", description = "Internal lookup used by other services.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Member found",
            content = @Content(schema = @Schema(implementation = Member.class))),
        @ApiResponse(responseCode = "404", description = "Member not found")
    })
    @GetMapping("/by-email/{email}")
    public Member getByEmail(
            @Parameter(description = "Member email address", example = "john@example.com")
            @PathVariable String email) {
        return service.getMemberByEmail(email);
    }

    // ── Health check ──────────────────────────────────────────────────────────

    @Operation(summary = "Health check", description = "Liveness probe.")
    @ApiResponse(responseCode = "200", description = "Service is up")
    @GetMapping("/test")
    public String test() { return "Member Service Working ✅"; }
}

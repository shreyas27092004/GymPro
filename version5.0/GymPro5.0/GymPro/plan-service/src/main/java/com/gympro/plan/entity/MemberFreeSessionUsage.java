package com.gympro.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Tracks the ONE lifetime free trainer session granted to non-subscribed members.
 *
 * Rules:
 *   - Created the first time a non-subscribed member books a trainer session.
 *   - hasUsedFreeSession = true means the free quota is exhausted.
 *   - If the member later cancels that free booking, hasUsedFreeSession is reset to false
 *     (restored via restoreNonSubscribedFreeSession).
 *   - Once a member subscribes to a plan, this table is ignored; plan quota takes over.
 *
 * One row per member (UNIQUE constraint on member_id).
 */
@Entity
@Table(name = "member_free_session_usage",
       uniqueConstraints = @UniqueConstraint(columnNames = "member_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberFreeSessionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    /**
     * false = member has NOT yet used their 1 free session (eligible)
     * true  = member HAS used their 1 free session (must pay for all future sessions)
     */
    @Column(name = "has_used_free_session", nullable = false)
    private boolean hasUsedFreeSession = false;

    /** Timestamp when the free session was consumed (null if not yet used). */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** The booking ID that consumed the free session (for audit trail). */
    @Column(name = "booking_id")
    private Long bookingId;
}

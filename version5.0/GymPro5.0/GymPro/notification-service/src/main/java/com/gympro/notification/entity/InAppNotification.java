package com.gympro.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persistent in-app notification record.
 *
 * Changes from original:
 *  - Added `referenceId` + `referenceType` for deduplication (prevents duplicate
 *    notifications for the same event e.g. bookingId=42/BOOKING_CONFIRMED).
 *  - Added `uniqueKey` unique constraint to prevent duplicates at DB level.
 *  - Added `deletedAt` soft-delete support.
 */
@Entity
@Table(
    name = "in_app_notifications",
    indexes = {
        @Index(name = "idx_user_created",  columnList = "userId, createdAt DESC"),
        @Index(name = "idx_user_unread",   columnList = "userId, isRead"),
        @Index(name = "idx_user_role",     columnList = "userId, userRole")
    },
    uniqueConstraints = {
        // Prevents duplicate notifications: same user + same event + same reference object
        @UniqueConstraint(
            name = "uq_notification_dedup",
            columnNames = {"userId", "eventType", "referenceId"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InAppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Maps to member / trainer / admin ID in their respective service DBs. */
    @Column(nullable = false)
    private Long userId;

    /** "MEMBER", "TRAINER", or "ADMIN" */
    @Column(nullable = false, length = 20)
    private String userRole;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    /** One of: BOOKING | PAYMENT | PLAN | SYSTEM */
    @Column(nullable = false, length = 20)
    private String type;

    /**
     * Fine-grained event type used for deduplication.
     * e.g. "BOOKING_CONFIRMED", "PAYMENT_SUCCESS", "PLAN_ACTIVATED"
     */
    @Column(nullable = false, length = 40)
    private String eventType;

    /**
     * ID of the referenced domain object (bookingId, paymentId, etc.).
     * Used together with eventType for deduplication.
     * Null for SYSTEM notifications.
     */
    @Column
    private Long referenceId;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Soft-delete timestamp. Null = active. Non-null = deleted by user. */
    @Column
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
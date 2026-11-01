package com.gympro.notification.repository;

import com.gympro.notification.entity.InAppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    // ── Paginated queries (soft-delete aware) ─────────────────────────────────

    /** All active (non-deleted) notifications for a user, newest first — paginated. */
    Page<InAppNotification> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** All active notifications for a user (no pagination, for legacy callers). */
    List<InAppNotification> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    /** Role-filtered paginated query (for admin-targeted notifications). */
    Page<InAppNotification> findByUserIdAndUserRoleAndDeletedAtIsNullOrderByCreatedAtDesc(
        Long userId, String userRole, Pageable pageable);

    // ── Unread queries ─────────────────────────────────────────────────────────

    /** Only unread, non-deleted notifications for a user, newest first. */
    List<InAppNotification> findByUserIdAndIsReadFalseAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    /** Count of unread, non-deleted notifications. */
    long countByUserIdAndIsReadFalseAndDeletedAtIsNull(Long userId);

    // ── Bulk update ───────────────────────────────────────────────────────────

    /** Bulk-mark all of a user's unread active notifications as read. */
    @Modifying
    @Query("UPDATE InAppNotification n SET n.isRead = true " +
           "WHERE n.userId = :userId AND n.isRead = false AND n.deletedAt IS NULL")
    int markAllReadByUserId(@Param("userId") Long userId);

    // ── Soft delete ───────────────────────────────────────────────────────────

    /** Soft-delete a single notification (only if it belongs to the given user). */
    @Modifying
    @Query("UPDATE InAppNotification n SET n.deletedAt = CURRENT_TIMESTAMP " +
           "WHERE n.id = :id AND n.userId = :userId AND n.deletedAt IS NULL")
    int softDeleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /** Soft-delete ALL notifications for a user. */
    @Modifying
    @Query("UPDATE InAppNotification n SET n.deletedAt = CURRENT_TIMESTAMP " +
           "WHERE n.userId = :userId AND n.deletedAt IS NULL")
    int softDeleteAllByUserId(@Param("userId") Long userId);

    // ── Deduplication check ───────────────────────────────────────────────────

    /**
     * Check if a notification already exists for the same user + event + reference.
     * Used to prevent duplicate notifications from RabbitMQ retry storms.
     */
    boolean existsByUserIdAndEventTypeAndReferenceId(Long userId, String eventType, Long referenceId);

    /**
     * Find existing notification for deduplication (when referenceId is null, e.g. SYSTEM events).
     * Uses eventType + userId + title match within last 5 minutes.
     */
    @Query("SELECT n FROM InAppNotification n " +
           "WHERE n.userId = :userId AND n.eventType = :eventType " +
           "AND n.referenceId IS NULL AND n.createdAt >= :since")
    Optional<InAppNotification> findRecentSystemNotification(
        @Param("userId") Long userId,
        @Param("eventType") String eventType,
        @Param("since") java.time.LocalDateTime since);
}
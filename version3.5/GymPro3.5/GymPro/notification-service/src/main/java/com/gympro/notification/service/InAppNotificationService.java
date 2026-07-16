package com.gympro.notification.service;

import com.gympro.notification.dto.NotificationDtos.*;
import com.gympro.notification.entity.InAppNotification;
import com.gympro.notification.repository.InAppNotificationRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Core service for in-app notifications.
 *
 * Features:
 *  - Persistent DB-backed notifications with pagination
 *  - Deduplication: same userId + eventType + referenceId = one notification
 *  - SSE (Server-Sent Events) for real-time push to connected clients
 *  - Soft-delete: notifications are hidden, not physically removed
 *  - Role-based filtering for admin notifications
 */
@Service
public class InAppNotificationService {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationService.class);
    private static final int  DEFAULT_PAGE_SIZE = 20;
    private static final long SSE_TIMEOUT_MS    = 5L * 60 * 1000; // 5 minutes — must be long for SseEmitter(long)

    @Autowired
    private InAppNotificationRepository repo;

    // ── SSE emitter registry: userId → list of active SSE connections ─────────
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // ═════════════════════════════════════════════════════════════════════════
    //  SSE — real-time push
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Create and register an SSE emitter for the given user.
     * Frontend calls GET /notify/inapp/{userId}/stream to connect.
     */
    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Cleanup callbacks
        Runnable cleanup = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // Send initial unread count immediately on connect
        try {
            long unread = repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId);
            emitter.send(SseEmitter.event()
                .name("UNREAD_COUNT")
                .data(Map.of("count", unread, "userId", userId)));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE event to userId={}", userId);
        }

        log.debug("SSE emitter registered for userId={}, total emitters={}", userId,
                  emitters.getOrDefault(userId, new CopyOnWriteArrayList<>()).size());
        return emitter;
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emitters.remove(userId);
        }
    }

    /** Push a new notification event to all active SSE connections for a user. */
    private void pushToUser(Long userId, InAppNotification saved) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) return;

        NotificationResponse payload = toResponse(saved);
        long unread = repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId);

        List<SseEmitter> dead = new java.util.ArrayList<>();
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("NEW_NOTIFICATION")
                    .data(payload));
                emitter.send(SseEmitter.event()
                    .name("UNREAD_COUNT")
                    .data(Map.of("count", unread, "userId", userId)));
            } catch (IOException e) {
                log.debug("Dead SSE emitter for userId={}, removing.", userId);
                dead.add(emitter);
            }
        }
        userEmitters.removeAll(dead);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CREATE
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Create and persist a new in-app notification.
     * Performs deduplication: if userId + eventType + referenceId already exists,
     * returns the existing notification instead of creating a duplicate.
     *
     * Called by NotificationConsumer for every RabbitMQ event.
     *
     * @param userId       recipient's DB user ID
     * @param userRole     "MEMBER" | "TRAINER" | "ADMIN"
     * @param title        short title (shown bold in UI)
     * @param message      body message
     * @param type         notification category: BOOKING | PAYMENT | PLAN | SYSTEM
     * @param eventType    fine-grained event: BOOKING_CONFIRMED, PAYMENT_SUCCESS, etc.
     * @param referenceId  domain object ID (bookingId, paymentId…), null for SYSTEM
     * @return the saved (or existing deduplicated) notification
     */
    public InAppNotification createNotification(Long userId,
                                                 String userRole,
                                                 String title,
                                                 String message,
                                                 String type,
                                                 String eventType,
                                                 Long referenceId) {
        // ── Deduplication check ───────────────────────────────────────────────
        if (referenceId != null &&
            repo.existsByUserIdAndEventTypeAndReferenceId(userId, eventType, referenceId)) {
            log.debug("🔁 Dedup: skip duplicate notification userId={}, eventType={}, refId={}",
                      userId, eventType, referenceId);
            // Return a lightweight stub — consumer doesn't use the return value
            return InAppNotification.builder()
                .userId(userId).eventType(eventType).referenceId(referenceId).build();
        }

        // ── For null referenceId (SYSTEM), check within last 5 min ────────────
        if (referenceId == null) {
            boolean recentExists = repo.findRecentSystemNotification(
                userId, eventType, LocalDateTime.now().minusMinutes(5)).isPresent();
            if (recentExists) {
                log.debug("🔁 Dedup: skip recent SYSTEM notification userId={}, eventType={}",
                          userId, eventType);
                return InAppNotification.builder().userId(userId).eventType(eventType).build();
            }
        }

        // ── Persist ───────────────────────────────────────────────────────────
        InAppNotification notif = InAppNotification.builder()
            .userId(userId)
            .userRole(userRole)
            .title(title)
            .message(message)
            .type(type)
            .eventType(eventType)
            .referenceId(referenceId)
            .isRead(false)
            .build();

        InAppNotification saved = repo.save(notif);
        log.info("📬 InApp saved: id={}, userId={}, type={}, event={}", saved.getId(), userId, type, eventType);

        // ── Real-time push via SSE ────────────────────────────────────────────
        pushToUser(userId, saved);

        return saved;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  READ — paginated
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Paginated list of notifications for a user.
     *
     * @param userId   the user
     * @param page     zero-based page index
     * @param size     page size (default 20, max 100)
     * @return a fully populated page response
     */
    public NotificationPageResponse getNotificationsPage(Long userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);

        Page<InAppNotification> dbPage =
            repo.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, pageable);

        long unread = repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId);

        return NotificationPageResponse.builder()
            .notifications(dbPage.getContent().stream()
                .map(this::toResponse).collect(Collectors.toList()))
            .totalElements(dbPage.getTotalElements())
            .totalPages(dbPage.getTotalPages())
            .currentPage(dbPage.getNumber())
            .pageSize(dbPage.getSize())
            .hasNext(dbPage.hasNext())
            .hasPrevious(dbPage.hasPrevious())
            .unreadCount(unread)
            .build();
    }

    /** All notifications for a user (no pagination — for backward compat). */
    public List<NotificationResponse> getNotificationsForUser(Long userId) {
        return repo.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Count of unread notifications. */
    public long getUnreadCount(Long userId) {
        return repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UPDATE — mark read
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Mark a single notification as read.
     * Returns false if not found or already deleted.
     */
    @Transactional
    public boolean markAsRead(Long notificationId) {
        return repo.findById(notificationId)
            .filter(n -> !n.isDeleted())
            .map(n -> {
                if (!n.isRead()) {
                    n.setRead(true);
                    repo.save(n);
                    log.debug("✅ Marked notification {} as read", notificationId);
                }
                return true;
            }).orElseGet(() -> {
                log.warn("⚠️  markAsRead: id={} not found or deleted", notificationId);
                return false;
            });
    }

    /**
     * Bulk-mark ALL of a user's unread notifications as read.
     * Returns the number of rows updated.
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        int updated = repo.markAllReadByUserId(userId);
        log.debug("✅ Marked {} notifications as read for userId={}", updated, userId);
        return updated;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DELETE (soft)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Soft-delete a single notification.
     * Only the notification owner can delete their own notification.
     */
    @Transactional
    public boolean deleteNotification(Long notificationId, Long userId) {
        int updated = repo.softDeleteByIdAndUserId(notificationId, userId);
        if (updated > 0) {
            log.debug("🗑️  Soft-deleted notification id={} for userId={}", notificationId, userId);
            return true;
        }
        log.warn("⚠️  deleteNotification: id={} not found for userId={}", notificationId, userId);
        return false;
    }

    /**
     * Soft-delete ALL notifications for a user (clear all).
     */
    @Transactional
    public int deleteAllNotifications(Long userId) {
        int deleted = repo.softDeleteAllByUserId(userId);
        log.debug("🗑️  Soft-deleted {} notifications for userId={}", deleted, userId);
        return deleted;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  MAPPER
    // ═════════════════════════════════════════════════════════════════════════

    public NotificationResponse toResponse(InAppNotification n) {
        return NotificationResponse.builder()
            .id(n.getId())
            .userId(n.getUserId())
            .userRole(n.getUserRole())
            .title(n.getTitle())
            .message(n.getMessage())
            .type(n.getType())
            .eventType(n.getEventType())
            .referenceId(n.getReferenceId())
            .isRead(n.isRead())
            .createdAt(n.getCreatedAt())
            .build();
    }
}
package com.gympro.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO classes for InApp Notification REST API responses.
 * Using inner static classes to keep them co-located.
 */
public class NotificationDtos {

    // ── Single notification response ──────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NotificationResponse {
        private Long id;
        private Long userId;
        private String userRole;
        private String title;
        private String message;
        private String type;
        private String eventType;
        private Long referenceId;
        private boolean isRead;
        private LocalDateTime createdAt;
    }

    // ── Paginated notification list response ──────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPageResponse {
        private List<NotificationResponse> notifications;
        private long totalElements;
        private int totalPages;
        private int currentPage;
        private int pageSize;
        private boolean hasNext;
        private boolean hasPrevious;
        private long unreadCount;
    }

    // ── Unread count response ─────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnreadCountResponse {
        private long count;
        private Long userId;
    }

    // ── Bulk action response ──────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkActionResponse {
        private boolean success;
        private int affected;
        private String message;
    }

    // ── Single action response ────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionResponse {
        private boolean success;
        private Long notificationId;
        private String message;
    }

    // ── SSE / WebSocket push event ────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NotificationPushEvent {
        private String eventName;   // "NEW_NOTIFICATION" | "UNREAD_COUNT_UPDATE"
        private NotificationResponse notification;
        private Long unreadCount;
        private Long userId;
    }
}
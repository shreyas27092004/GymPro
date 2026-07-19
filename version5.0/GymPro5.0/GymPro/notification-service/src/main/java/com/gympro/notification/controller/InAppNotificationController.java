package com.gympro.notification.controller;

import com.gympro.notification.dto.NotificationDtos.*;
import com.gympro.notification.service.InAppNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * REST API for in-app notifications.
 *
 * All endpoints under /notify/inapp/** — JWT is validated by the Gateway before
 * forwarding here, so no auth logic needed in this service.
 *
 * ┌────────────────────────────────────────────────────────────────────────┐
 * │  Endpoint                                    Method  Description       │
 * ├────────────────────────────────────────────────────────────────────────┤
 * │  /notify/inapp/{userId}                      GET     paginated list    │
 * │  /notify/inapp/{userId}/all                  GET     all (no page)     │
 * │  /notify/inapp/{userId}/unread-count         GET     unread badge      │
 * │  /notify/inapp/{userId}/stream               GET     SSE real-time     │
 * │  /notify/inapp/{notificationId}/read         PUT     mark one read     │
 * │  /notify/inapp/{userId}/read-all             PUT     mark all read     │
 * │  /notify/inapp/{notificationId}/{userId}     DELETE  delete one        │
 * │  /notify/inapp/{userId}/clear                DELETE  delete all        │
 * └────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/notify/inapp")
@Tag(name = "In-App Notifications", description = "Persistent in-app notification store + SSE real-time stream")
@CrossOrigin(origins = "*")
public class InAppNotificationController {

    @Autowired
    private InAppNotificationService inAppService;

    // ── GET /notify/inapp/{userId}?page=0&size=20 ─────────────────────────────

    @GetMapping("/{userId}")
    @Operation(summary = "Get paginated notifications for a user (newest first)")
    public ResponseEntity<NotificationPageResponse> getNotificationsPaged(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        NotificationPageResponse response = inAppService.getNotificationsPage(userId, page, size);
        return ResponseEntity.ok(response);
    }

    // ── GET /notify/inapp/{userId}/all ────────────────────────────────────────

    @GetMapping("/{userId}/all")
    @Operation(summary = "Get all notifications for a user (no pagination — use sparingly)")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications(
            @PathVariable Long userId) {

        return ResponseEntity.ok(inAppService.getNotificationsForUser(userId));
    }

    // ── GET /notify/inapp/{userId}/unread-count ───────────────────────────────

    @GetMapping("/{userId}/unread-count")
    @Operation(summary = "Get unread notification count for a user (lightweight badge poll)")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@PathVariable Long userId) {
        long count = inAppService.getUnreadCount(userId);
        return ResponseEntity.ok(UnreadCountResponse.builder()
            .count(count).userId(userId).build());
    }

    // ── GET /notify/inapp/{userId}/stream  (SSE) ──────────────────────────────

    @GetMapping(value = "/{userId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE real-time stream — subscribe to receive instant push notifications")
    public SseEmitter subscribeToNotifications(
            @PathVariable Long userId,
            @Parameter(description = "Optional role filter e.g. ADMIN, MEMBER, TRAINER")
            @RequestParam(required = false) String role) {

        return inAppService.createEmitter(userId);
    }

    // ── PUT /notify/inapp/{notificationId}/read ───────────────────────────────

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<ActionResponse> markAsRead(@PathVariable Long notificationId) {
        boolean updated = inAppService.markAsRead(notificationId);
        if (updated) {
            return ResponseEntity.ok(ActionResponse.builder()
                .success(true).notificationId(notificationId)
                .message("Notification marked as read").build());
        }
        return ResponseEntity.notFound().build();
    }

    // ── PUT /notify/inapp/{userId}/read-all ───────────────────────────────────

    @PutMapping("/{userId}/read-all")
    @Operation(summary = "Mark ALL notifications as read for a user")
    public ResponseEntity<BulkActionResponse> markAllAsRead(@PathVariable Long userId) {
        int updated = inAppService.markAllAsRead(userId);
        return ResponseEntity.ok(BulkActionResponse.builder()
            .success(true).affected(updated)
            .message(updated + " notifications marked as read").build());
    }

    // ── DELETE /notify/inapp/{notificationId}/{userId} ────────────────────────

    @DeleteMapping("/{notificationId}/{userId}")
    @Operation(summary = "Soft-delete a single notification (must belong to userId)")
    public ResponseEntity<ActionResponse> deleteNotification(
            @PathVariable Long notificationId,
            @PathVariable Long userId) {

        boolean deleted = inAppService.deleteNotification(notificationId, userId);
        if (deleted) {
            return ResponseEntity.ok(ActionResponse.builder()
                .success(true).notificationId(notificationId)
                .message("Notification deleted").build());
        }
        return ResponseEntity.notFound().build();
    }

    // ── DELETE /notify/inapp/{userId}/clear ───────────────────────────────────

    @DeleteMapping("/{userId}/clear")
    @Operation(summary = "Soft-delete ALL notifications for a user")
    public ResponseEntity<BulkActionResponse> clearAllNotifications(@PathVariable Long userId) {
        int deleted = inAppService.deleteAllNotifications(userId);
        return ResponseEntity.ok(BulkActionResponse.builder()
            .success(true).affected(deleted)
            .message("All " + deleted + " notifications cleared").build());
    }
}
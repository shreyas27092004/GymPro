package com.gympro.notification.controller;

import com.gympro.notification.dto.NotificationDtos.*;
import com.gympro.notification.service.InAppNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InAppNotificationController}.
 * The {@link InAppNotificationService} dependency is mocked.
 */
@ExtendWith(MockitoExtension.class)
class InAppNotificationControllerTest {

    @Mock
    private InAppNotificationService inAppService;

    private InAppNotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new InAppNotificationController();
        // The controller uses field injection (@Autowired on a private field).
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "inAppService", inAppService);
    }

    @Test
    @DisplayName("GET /notify/inapp/{userId} returns the paginated response")
    void getNotificationsPaged_returnsPageResponse() {
        NotificationPageResponse page = NotificationPageResponse.builder()
                .notifications(List.of()).totalElements(0L).totalPages(0)
                .currentPage(0).pageSize(20).hasNext(false).hasPrevious(false).unreadCount(0L)
                .build();
        when(inAppService.getNotificationsPage(10L, 0, 20)).thenReturn(page);

        ResponseEntity<NotificationPageResponse> response = controller.getNotificationsPaged(10L, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
    }

    @Test
    @DisplayName("GET /notify/inapp/{userId}/all returns the full unpaginated list")
    void getAllNotifications_returnsList() {
        NotificationResponse item = NotificationResponse.builder().id(1L).build();
        when(inAppService.getNotificationsForUser(10L)).thenReturn(List.of(item));

        ResponseEntity<List<NotificationResponse>> response = controller.getAllNotifications(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(item);
    }

    @Test
    @DisplayName("GET /notify/inapp/{userId}/unread-count returns count and userId")
    void getUnreadCount_returnsCountAndUserId() {
        when(inAppService.getUnreadCount(10L)).thenReturn(5L);

        ResponseEntity<UnreadCountResponse> response = controller.getUnreadCount(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCount()).isEqualTo(5L);
        assertThat(response.getBody().getUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("GET /notify/inapp/{userId}/stream delegates to createEmitter")
    void subscribeToNotifications_delegatesToService() {
        SseEmitter emitter = new SseEmitter();
        when(inAppService.createEmitter(10L)).thenReturn(emitter);

        SseEmitter result = controller.subscribeToNotifications(10L, "MEMBER");

        assertThat(result).isSameAs(emitter);
        verify(inAppService, times(1)).createEmitter(10L);
    }

    @Test
    @DisplayName("GET /notify/inapp/{userId}/stream works with a null role filter")
    void subscribeToNotifications_nullRole_stillWorks() {
        SseEmitter emitter = new SseEmitter();
        when(inAppService.createEmitter(10L)).thenReturn(emitter);

        SseEmitter result = controller.subscribeToNotifications(10L, null);

        assertThat(result).isSameAs(emitter);
    }

    @Test
    @DisplayName("PUT /notify/inapp/{notificationId}/read returns 200 when updated")
    void markAsRead_updated_returns200() {
        when(inAppService.markAsRead(1L)).thenReturn(true);

        ResponseEntity<ActionResponse> response = controller.markAsRead(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getNotificationId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("PUT /notify/inapp/{notificationId}/read returns 404 when not found")
    void markAsRead_notFound_returns404() {
        when(inAppService.markAsRead(99L)).thenReturn(false);

        ResponseEntity<ActionResponse> response = controller.markAsRead(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PUT /notify/inapp/{userId}/read-all returns the count of affected rows")
    void markAllAsRead_returnsAffectedCount() {
        when(inAppService.markAllAsRead(10L)).thenReturn(3);

        ResponseEntity<BulkActionResponse> response = controller.markAllAsRead(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAffected()).isEqualTo(3);
        assertThat(response.getBody().getMessage()).contains("3");
    }

    @Test
    @DisplayName("DELETE /notify/inapp/{notificationId}/{userId} returns 200 when deleted")
    void deleteNotification_deleted_returns200() {
        when(inAppService.deleteNotification(1L, 10L)).thenReturn(true);

        ResponseEntity<ActionResponse> response = controller.deleteNotification(1L, 10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    @DisplayName("DELETE /notify/inapp/{notificationId}/{userId} returns 404 when not found")
    void deleteNotification_notFound_returns404() {
        when(inAppService.deleteNotification(99L, 10L)).thenReturn(false);

        ResponseEntity<ActionResponse> response = controller.deleteNotification(99L, 10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("DELETE /notify/inapp/{userId}/clear returns the count of cleared notifications")
    void clearAllNotifications_returnsDeletedCount() {
        when(inAppService.deleteAllNotifications(10L)).thenReturn(8);

        ResponseEntity<BulkActionResponse> response = controller.clearAllNotifications(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAffected()).isEqualTo(8);
        assertThat(response.getBody().getMessage()).contains("8");
    }
}

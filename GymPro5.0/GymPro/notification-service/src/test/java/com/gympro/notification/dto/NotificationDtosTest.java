package com.gympro.notification.dto;

import com.gympro.notification.dto.NotificationDtos.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link NotificationDtos} inner DTO classes.
 * Verifies Lombok-generated builders, getters, setters, equals/hashCode and toString.
 */
class NotificationDtosTest {

    @Test
    @DisplayName("NotificationResponse builder sets all fields")
    void notificationResponse_builder_setsAllFields() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);

        NotificationResponse response = NotificationResponse.builder()
                .id(1L).userId(2L).userRole("MEMBER").title("Title")
                .message("Message").type("BOOKING").eventType("BOOKING_CONFIRMED")
                .referenceId(3L).isRead(true).createdAt(now)
                .build();

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.getUserRole()).isEqualTo("MEMBER");
        assertThat(response.getTitle()).isEqualTo("Title");
        assertThat(response.getMessage()).isEqualTo("Message");
        assertThat(response.getType()).isEqualTo("BOOKING");
        assertThat(response.getEventType()).isEqualTo("BOOKING_CONFIRMED");
        assertThat(response.getReferenceId()).isEqualTo(3L);
        assertThat(response.isRead()).isTrue();
        assertThat(response.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("NotificationResponse no-args constructor and setters work")
    void notificationResponse_noArgsAndSetters() {
        NotificationResponse response = new NotificationResponse();
        response.setId(5L);
        response.setRead(false);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.isRead()).isFalse();
    }

    @Test
    @DisplayName("NotificationResponse equals/hashCode consistent for equal values")
    void notificationResponse_equalsAndHashCode() {
        NotificationResponse a = NotificationResponse.builder().id(1L).title("T").build();
        NotificationResponse b = NotificationResponse.builder().id(1L).title("T").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("NotificationPageResponse builder sets all fields")
    void notificationPageResponse_builder_setsAllFields() {
        NotificationResponse item = NotificationResponse.builder().id(1L).build();

        NotificationPageResponse page = NotificationPageResponse.builder()
                .notifications(List.of(item))
                .totalElements(1L).totalPages(1).currentPage(0).pageSize(20)
                .hasNext(false).hasPrevious(false).unreadCount(1L)
                .build();

        assertThat(page.getNotifications()).containsExactly(item);
        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getCurrentPage()).isEqualTo(0);
        assertThat(page.getPageSize()).isEqualTo(20);
        assertThat(page.isHasNext()).isFalse();
        assertThat(page.isHasPrevious()).isFalse();
        assertThat(page.getUnreadCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("UnreadCountResponse builder and getters work")
    void unreadCountResponse_builder() {
        UnreadCountResponse response = UnreadCountResponse.builder().count(5L).userId(10L).build();

        assertThat(response.getCount()).isEqualTo(5L);
        assertThat(response.getUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("BulkActionResponse builder and getters work")
    void bulkActionResponse_builder() {
        BulkActionResponse response = BulkActionResponse.builder()
                .success(true).affected(3).message("3 updated").build();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getAffected()).isEqualTo(3);
        assertThat(response.getMessage()).isEqualTo("3 updated");
    }

    @Test
    @DisplayName("ActionResponse builder and getters work")
    void actionResponse_builder() {
        ActionResponse response = ActionResponse.builder()
                .success(true).notificationId(7L).message("done").build();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getNotificationId()).isEqualTo(7L);
        assertThat(response.getMessage()).isEqualTo("done");
    }

    @Test
    @DisplayName("NotificationPushEvent builder and getters work")
    void notificationPushEvent_builder() {
        NotificationResponse notif = NotificationResponse.builder().id(1L).build();

        NotificationPushEvent event = NotificationPushEvent.builder()
                .eventName("NEW_NOTIFICATION").notification(notif).unreadCount(2L).userId(9L)
                .build();

        assertThat(event.getEventName()).isEqualTo("NEW_NOTIFICATION");
        assertThat(event.getNotification()).isEqualTo(notif);
        assertThat(event.getUnreadCount()).isEqualTo(2L);
        assertThat(event.getUserId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("All-args constructors work for every DTO")
    void allArgsConstructors_work() {
        LocalDateTime now = LocalDateTime.now();
        NotificationResponse notif = new NotificationResponse(1L, 2L, "MEMBER", "T", "M", "BOOKING", "EVT", 3L, true, now);
        assertThat(notif.getId()).isEqualTo(1L);

        NotificationPageResponse page = new NotificationPageResponse(List.of(notif), 1L, 1, 0, 20, false, false, 1L);
        assertThat(page.getTotalElements()).isEqualTo(1L);

        UnreadCountResponse unread = new UnreadCountResponse(2L, 5L);
        assertThat(unread.getCount()).isEqualTo(2L);

        BulkActionResponse bulk = new BulkActionResponse(true, 4, "msg");
        assertThat(bulk.getAffected()).isEqualTo(4);

        ActionResponse action = new ActionResponse(true, 6L, "msg");
        assertThat(action.getNotificationId()).isEqualTo(6L);

        NotificationPushEvent push = new NotificationPushEvent("EVT", notif, 1L, 9L);
        assertThat(push.getEventName()).isEqualTo("EVT");
    }
}

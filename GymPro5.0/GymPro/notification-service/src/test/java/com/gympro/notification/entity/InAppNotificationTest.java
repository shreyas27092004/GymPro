package com.gympro.notification.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InAppNotification}.
 * Covers builder/getters/setters, the {@code isDeleted()} helper, and the
 * {@code @PrePersist onCreate} lifecycle callback (invoked via reflection since
 * it's package-private and normally only triggered by JPA).
 */
class InAppNotificationTest {

    @Test
    @DisplayName("Builder sets all fields correctly")
    void builder_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        InAppNotification notification = InAppNotification.builder()
                .id(1L).userId(2L).userRole("MEMBER").title("Title")
                .message("Message").type("BOOKING").eventType("BOOKING_CONFIRMED")
                .referenceId(3L).isRead(false).createdAt(now)
                .build();

        assertThat(notification.getId()).isEqualTo(1L);
        assertThat(notification.getUserId()).isEqualTo(2L);
        assertThat(notification.getUserRole()).isEqualTo("MEMBER");
        assertThat(notification.getTitle()).isEqualTo("Title");
        assertThat(notification.getMessage()).isEqualTo("Message");
        assertThat(notification.getType()).isEqualTo("BOOKING");
        assertThat(notification.getEventType()).isEqualTo("BOOKING_CONFIRMED");
        assertThat(notification.getReferenceId()).isEqualTo(3L);
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Builder default sets isRead to false when omitted")
    void builder_defaultsIsReadToFalse() {
        InAppNotification notification = InAppNotification.builder()
                .userId(1L).userRole("MEMBER").title("T").message("M")
                .type("SYSTEM").eventType("EVT")
                .build();

        assertThat(notification.isRead()).isFalse();
    }

    @Test
    @DisplayName("isDeleted returns false when deletedAt is null")
    void isDeleted_falseWhenDeletedAtNull() {
        InAppNotification notification = new InAppNotification();
        notification.setDeletedAt(null);

        assertThat(notification.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("isDeleted returns true when deletedAt is set")
    void isDeleted_trueWhenDeletedAtSet() {
        InAppNotification notification = new InAppNotification();
        notification.setDeletedAt(LocalDateTime.now());

        assertThat(notification.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Setters update all fields")
    void setters_updateAllFields() {
        InAppNotification notification = new InAppNotification();
        LocalDateTime now = LocalDateTime.now();

        notification.setId(10L);
        notification.setUserId(20L);
        notification.setUserRole("ADMIN");
        notification.setTitle("New Title");
        notification.setMessage("New Message");
        notification.setType("PAYMENT");
        notification.setEventType("PAYMENT_SUCCESS");
        notification.setReferenceId(30L);
        notification.setRead(true);
        notification.setCreatedAt(now);
        notification.setDeletedAt(now);

        assertThat(notification.getId()).isEqualTo(10L);
        assertThat(notification.getUserId()).isEqualTo(20L);
        assertThat(notification.getUserRole()).isEqualTo("ADMIN");
        assertThat(notification.getTitle()).isEqualTo("New Title");
        assertThat(notification.getMessage()).isEqualTo("New Message");
        assertThat(notification.getType()).isEqualTo("PAYMENT");
        assertThat(notification.getEventType()).isEqualTo("PAYMENT_SUCCESS");
        assertThat(notification.getReferenceId()).isEqualTo(30L);
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getCreatedAt()).isEqualTo(now);
        assertThat(notification.getDeletedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("onCreate lifecycle callback sets createdAt to now")
    void onCreate_setsCreatedAt() throws Exception {
        InAppNotification notification = new InAppNotification();
        assertThat(notification.getCreatedAt()).isNull();

        Method onCreate = InAppNotification.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(notification);

        assertThat(notification.getCreatedAt()).isNotNull();
        assertThat(notification.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("equals and hashCode are consistent for identical field values")
    void equalsAndHashCode_consistent() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);
        InAppNotification a = InAppNotification.builder().id(1L).userId(2L).createdAt(now).build();
        InAppNotification b = InAppNotification.builder().id(1L).userId(2L).createdAt(now).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("toString returns a non-null, non-empty string")
    void toString_isNotBlank() {
        InAppNotification notification = InAppNotification.builder().id(1L).title("Title").build();

        assertThat(notification.toString()).isNotBlank().contains("Title");
    }
}

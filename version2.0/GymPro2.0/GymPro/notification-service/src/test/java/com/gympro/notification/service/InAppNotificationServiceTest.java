package com.gympro.notification.service;

import com.gympro.notification.dto.NotificationDtos.NotificationPageResponse;
import com.gympro.notification.dto.NotificationDtos.NotificationResponse;
import com.gympro.notification.entity.InAppNotification;
import com.gympro.notification.repository.InAppNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InAppNotificationService}.
 * The {@link InAppNotificationRepository} is mocked — no real database is used.
 */
@ExtendWith(MockitoExtension.class)
class InAppNotificationServiceTest {

    @Mock
    private InAppNotificationRepository repo;

    @InjectMocks
    private InAppNotificationService service;

    private InAppNotification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = InAppNotification.builder()
                .id(1L).userId(10L).userRole("MEMBER").title("Title")
                .message("Message").type("BOOKING").eventType("BOOKING_CONFIRMED")
                .referenceId(100L).isRead(false).createdAt(LocalDateTime.now())
                .build();
    }

    // ──────────────────────────────────────────────────────────────────
    // createNotification — deduplication + persistence
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createNotification")
    class CreateNotificationTests {

        @Test
        @DisplayName("Skips duplicate creation when userId+eventType+referenceId already exists")
        void createNotification_duplicateByReference_skipsCreation() {
            when(repo.existsByUserIdAndEventTypeAndReferenceId(10L, "BOOKING_CONFIRMED", 100L))
                    .thenReturn(true);

            InAppNotification result = service.createNotification(
                    10L, "MEMBER", "Title", "Message", "BOOKING", "BOOKING_CONFIRMED", 100L);

            assertThat(result.getUserId()).isEqualTo(10L);
            assertThat(result.getEventType()).isEqualTo("BOOKING_CONFIRMED");
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("Skips duplicate SYSTEM notification within the last 5 minutes when referenceId is null")
        void createNotification_recentSystemDuplicate_skipsCreation() {
            when(repo.findRecentSystemNotification(eq(10L), eq("SYSTEM_ALERT"), any()))
                    .thenReturn(Optional.of(sampleNotification));

            InAppNotification result = service.createNotification(
                    10L, "MEMBER", "Title", "Message", "SYSTEM", "SYSTEM_ALERT", null);

            assertThat(result.getUserId()).isEqualTo(10L);
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("Persists a new notification when no duplicate exists (with referenceId)")
        void createNotification_noDuplicate_persists() {
            when(repo.existsByUserIdAndEventTypeAndReferenceId(10L, "BOOKING_CONFIRMED", 100L))
                    .thenReturn(false);
            when(repo.save(any(InAppNotification.class))).thenAnswer(inv -> {
                InAppNotification n = inv.getArgument(0);
                n.setId(99L);
                return n;
            });
            when(repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(10L)).thenReturn(1L);

            InAppNotification result = service.createNotification(
                    10L, "MEMBER", "Session Confirmed", "Body", "BOOKING", "BOOKING_CONFIRMED", 100L);

            assertThat(result.getId()).isEqualTo(99L);
            assertThat(result.getTitle()).isEqualTo("Session Confirmed");
            assertThat(result.isRead()).isFalse();
            verify(repo, times(1)).save(any(InAppNotification.class));
        }

        @Test
        @DisplayName("Persists a new SYSTEM notification when referenceId is null and no recent duplicate")
        void createNotification_nullReferenceIdNoDuplicate_persists() {
            when(repo.findRecentSystemNotification(eq(10L), eq("SYSTEM_ALERT"), any()))
                    .thenReturn(Optional.empty());
            when(repo.save(any(InAppNotification.class))).thenAnswer(inv -> inv.getArgument(0));
            when(repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(10L)).thenReturn(0L);

            InAppNotification result = service.createNotification(
                    10L, "MEMBER", "Alert", "Body", "SYSTEM", "SYSTEM_ALERT", null);

            assertThat(result.getReferenceId()).isNull();
            verify(repo, times(1)).save(any(InAppNotification.class));
        }

        @Test
        @DisplayName("Does not push or fail when no SSE emitters are registered for the user")
        void createNotification_noEmittersRegistered_doesNotThrow() {
            when(repo.existsByUserIdAndEventTypeAndReferenceId(anyLong(), anyString(), anyLong()))
                    .thenReturn(false);
            when(repo.save(any(InAppNotification.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> service.createNotification(
                    10L, "MEMBER", "Title", "Message", "BOOKING", "BOOKING_CONFIRMED", 200L));
        }

        @Test
        @DisplayName("Pushes to active SSE emitters and removes dead ones on IOException")
        void createNotification_pushesToEmitters_removesDeadOnes() throws IOException {
            when(repo.existsByUserIdAndEventTypeAndReferenceId(anyLong(), anyString(), anyLong()))
                    .thenReturn(false);
            when(repo.save(any(InAppNotification.class))).thenAnswer(inv -> inv.getArgument(0));
            when(repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(10L)).thenReturn(2L);

            SseEmitter deadEmitter = emitterWithFailingHandler();
            registerEmitter(10L, deadEmitter);

            assertDoesNotThrow(() -> service.createNotification(
                    10L, "MEMBER", "Title", "Message", "BOOKING", "BOOKING_CONFIRMED", 300L));

            // The dead emitter should have been pruned from the internal registry.
            CopyOnWriteArrayList<SseEmitter> remaining = getEmittersMap().get(10L);
            assertThat(remaining == null || remaining.isEmpty()).isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // SSE emitter creation
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createEmitter")
    class CreateEmitterTests {

        @Test
        @DisplayName("Registers a new emitter and returns it")
        void createEmitter_registersAndReturns() {
            when(repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(10L)).thenReturn(3L);

            SseEmitter emitter = service.createEmitter(10L);

            assertThat(emitter).isNotNull();
            assertThat(getEmittersMap()).containsKey(10L);
            assertThat(getEmittersMap().get(10L)).contains(emitter);
        }

        @Test
        @DisplayName("Multiple emitters for the same user are all registered")
        void createEmitter_multipleForSameUser_allRegistered() {
            when(repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(10L)).thenReturn(0L);

            SseEmitter e1 = service.createEmitter(10L);
            SseEmitter e2 = service.createEmitter(10L);

            assertThat(getEmittersMap().get(10L)).containsExactlyInAnyOrder(e1, e2);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Pagination / read queries
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Read queries")
    class ReadQueryTests {

        @Test
        @DisplayName("getNotificationsPage returns a fully populated page response")
        void getNotificationsPage_returnsPopulatedResponse() {
            Page<InAppNotification> page = new PageImpl<>(List.of(sampleNotification),
                    PageRequest.of(0, 20), 1);
            when(repo.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                    .thenReturn(page);
            when(repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(10L)).thenReturn(1L);

            NotificationPageResponse response = service.getNotificationsPage(10L, 0, 20);

            assertThat(response.getNotifications()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getUnreadCount()).isEqualTo(1L);
            assertThat(response.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("getNotificationsPage clamps page size above 100 down to 100")
        void getNotificationsPage_clampsOversizedPageSize() {
            Page<InAppNotification> page = new PageImpl<>(List.of());
            when(repo.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                    .thenReturn(page);
            when(repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(10L)).thenReturn(0L);

            service.getNotificationsPage(10L, 0, 500);

            verify(repo).findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                    eq(10L), argThatPageSizeIs(100));
        }

        @Test
        @DisplayName("getNotificationsPage clamps page size below 1 up to 1")
        void getNotificationsPage_clampsUndersizedPageSize() {
            Page<InAppNotification> page = new PageImpl<>(List.of());
            when(repo.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                    .thenReturn(page);
            when(repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(10L)).thenReturn(0L);

            service.getNotificationsPage(10L, 0, 0);

            verify(repo).findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                    eq(10L), argThatPageSizeIs(1));
        }

        @Test
        @DisplayName("getNotificationsPage clamps a negative page index to 0")
        void getNotificationsPage_clampsNegativePage() {
            Page<InAppNotification> page = new PageImpl<>(List.of());
            when(repo.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                    .thenReturn(page);
            when(repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(10L)).thenReturn(0L);

            service.getNotificationsPage(10L, -5, 20);

            verify(repo).findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                    eq(10L), argThatPageNumberIs(0));
        }

        @Test
        @DisplayName("getNotificationsForUser returns all mapped notifications")
        void getNotificationsForUser_returnsMappedList() {
            when(repo.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(10L))
                    .thenReturn(List.of(sampleNotification));

            List<NotificationResponse> result = service.getNotificationsForUser(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getUnreadCount delegates to the repository")
        void getUnreadCount_delegatesToRepo() {
            when(repo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(10L)).thenReturn(7L);

            long count = service.getUnreadCount(10L);

            assertThat(count).isEqualTo(7L);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // markAsRead / markAllAsRead
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAsRead / markAllAsRead")
    class MarkReadTests {

        @Test
        @DisplayName("markAsRead marks an unread notification as read and saves it")
        void markAsRead_unreadNotification_marksAndSaves() {
            sampleNotification.setRead(false);
            when(repo.findById(1L)).thenReturn(Optional.of(sampleNotification));

            boolean result = service.markAsRead(1L);

            assertThat(result).isTrue();
            assertThat(sampleNotification.isRead()).isTrue();
            verify(repo, times(1)).save(sampleNotification);
        }

        @Test
        @DisplayName("markAsRead is a no-op save when already read, but still returns true")
        void markAsRead_alreadyRead_doesNotResave() {
            sampleNotification.setRead(true);
            when(repo.findById(1L)).thenReturn(Optional.of(sampleNotification));

            boolean result = service.markAsRead(1L);

            assertThat(result).isTrue();
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("markAsRead returns false when notification does not exist")
        void markAsRead_notFound_returnsFalse() {
            when(repo.findById(99L)).thenReturn(Optional.empty());

            boolean result = service.markAsRead(99L);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("markAsRead returns false when notification is soft-deleted")
        void markAsRead_softDeleted_returnsFalse() {
            sampleNotification.setDeletedAt(LocalDateTime.now());
            when(repo.findById(1L)).thenReturn(Optional.of(sampleNotification));

            boolean result = service.markAsRead(1L);

            assertThat(result).isFalse();
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("markAllAsRead returns the number of rows updated by the repository")
        void markAllAsRead_returnsUpdatedCount() {
            when(repo.markAllReadByUserId(10L)).thenReturn(4);

            int updated = service.markAllAsRead(10L);

            assertThat(updated).isEqualTo(4);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Soft delete
    // ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteNotification / deleteAllNotifications")
    class DeleteTests {

        @Test
        @DisplayName("deleteNotification returns true when a row was soft-deleted")
        void deleteNotification_success_returnsTrue() {
            when(repo.softDeleteByIdAndUserId(1L, 10L)).thenReturn(1);

            boolean result = service.deleteNotification(1L, 10L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("deleteNotification returns false when no matching row was found")
        void deleteNotification_notFound_returnsFalse() {
            when(repo.softDeleteByIdAndUserId(99L, 10L)).thenReturn(0);

            boolean result = service.deleteNotification(99L, 10L);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("deleteAllNotifications returns the number of rows soft-deleted")
        void deleteAllNotifications_returnsDeletedCount() {
            when(repo.softDeleteAllByUserId(10L)).thenReturn(6);

            int deleted = service.deleteAllNotifications(10L);

            assertThat(deleted).isEqualTo(6);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Mapper
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toResponse maps every entity field to the response DTO")
    void toResponse_mapsAllFields() {
        NotificationResponse response = service.toResponse(sampleNotification);

        assertThat(response.getId()).isEqualTo(sampleNotification.getId());
        assertThat(response.getUserId()).isEqualTo(sampleNotification.getUserId());
        assertThat(response.getUserRole()).isEqualTo(sampleNotification.getUserRole());
        assertThat(response.getTitle()).isEqualTo(sampleNotification.getTitle());
        assertThat(response.getMessage()).isEqualTo(sampleNotification.getMessage());
        assertThat(response.getType()).isEqualTo(sampleNotification.getType());
        assertThat(response.getEventType()).isEqualTo(sampleNotification.getEventType());
        assertThat(response.getReferenceId()).isEqualTo(sampleNotification.getReferenceId());
        assertThat(response.isRead()).isEqualTo(sampleNotification.isRead());
        assertThat(response.getCreatedAt()).isEqualTo(sampleNotification.getCreatedAt());
    }

    // ──────────────────────────────────────────────────────────────────
    // Test helpers
    // ──────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<Long, CopyOnWriteArrayList<SseEmitter>> getEmittersMap() {
        return (Map<Long, CopyOnWriteArrayList<SseEmitter>>) ReflectionTestUtils.getField(service, "emitters");
    }

    private void registerEmitter(Long userId, SseEmitter emitter) {
        getEmittersMap().computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    /** Builds an SseEmitter whose underlying handler always throws IOException on send. */
    private SseEmitter emitterWithFailingHandler() throws IOException {
        SseEmitter emitter = new SseEmitter();
        ResponseBodyEmitter.Handler failingHandler = new ResponseBodyEmitter.Handler() {
            @Override
            public void send(Object data, MediaType mediaType) throws IOException {
                throw new IOException("simulated dead connection");
            }

            @Override
            public void complete() {
            }

            @Override
            public void completeWithError(Throwable failure) {
            }

            @Override
            public void onTimeout(Runnable callback) {
            }

            @Override
            public void onError(Consumer<Throwable> callback) {
            }

            @Override
            public void onCompletion(Runnable callback) {
            }
        };
        emitter.initialize(failingHandler);
        return emitter;
    }

    private Pageable argThatPageSizeIs(int expectedSize) {
        return org.mockito.ArgumentMatchers.argThat(p -> p.getPageSize() == expectedSize);
    }

    private Pageable argThatPageNumberIs(int expectedPage) {
        return org.mockito.ArgumentMatchers.argThat(p -> p.getPageNumber() == expectedPage);
    }
}

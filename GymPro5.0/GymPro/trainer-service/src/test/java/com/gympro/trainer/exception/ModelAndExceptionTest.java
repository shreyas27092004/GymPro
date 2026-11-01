package com.gympro.trainer.exception;

import com.gympro.trainer.entity.Trainer;
import com.gympro.trainer.entity.TrainerSchedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Model and Exception Class Tests")
class ModelAndExceptionTest {

    // ── Trainer entity ──────────────────────────────────────────

    @Test
    @DisplayName("Trainer - default constructor creates empty object")
    void trainer_defaultConstructor_createsEmptyObject() {
        Trainer trainer = new Trainer();
        assertNull(trainer.getId());
        assertNull(trainer.getName());
        assertNull(trainer.getEmail());
        assertNull(trainer.getPhone());
        assertNull(trainer.getSpecialization());
        assertEquals(0, trainer.getExperienceYears());
        assertNull(trainer.getStatus());
    }

    @Test
    @DisplayName("Trainer - all-args constructor sets all fields")
    void trainer_allArgsConstructor_setsAllFields() {
        Trainer trainer = new Trainer(1L, "Alice", "alice@gym.com", "9999999999", "Pilates", 3, "ACTIVE", null);

        assertEquals(1L, trainer.getId());
        assertEquals("Alice", trainer.getName());
        assertEquals("alice@gym.com", trainer.getEmail());
        assertEquals("9999999999", trainer.getPhone());
        assertEquals("Pilates", trainer.getSpecialization());
        assertEquals(3, trainer.getExperienceYears());
        assertEquals("ACTIVE", trainer.getStatus());
        assertNull(trainer.getSessionFee());
    }

    @Test
    @DisplayName("Trainer - all-args constructor sets sessionFee")
    void trainer_allArgsConstructor_setsSessionFee() {
        Trainer trainer = new Trainer(1L, "Alice", "alice@gym.com", "9999999999", "Pilates", 3, "ACTIVE",
                new BigDecimal("500.00"));

        assertEquals(new BigDecimal("500.00"), trainer.getSessionFee());
    }

    @Test
    @DisplayName("Trainer - setters update fields correctly")
    void trainer_setters_updateFields() {
        Trainer trainer = new Trainer();
        trainer.setId(5L);
        trainer.setName("Bob");
        trainer.setEmail("bob@gym.com");
        trainer.setPhone("8888888888");
        trainer.setSpecialization("Strength");
        trainer.setExperienceYears(10);
        trainer.setStatus("INACTIVE");
        trainer.setSessionFee(new BigDecimal("750.00"));

        assertEquals(5L, trainer.getId());
        assertEquals("Bob", trainer.getName());
        assertEquals("bob@gym.com", trainer.getEmail());
        assertEquals("8888888888", trainer.getPhone());
        assertEquals("Strength", trainer.getSpecialization());
        assertEquals(10, trainer.getExperienceYears());
        assertEquals("INACTIVE", trainer.getStatus());
        assertEquals(new BigDecimal("750.00"), trainer.getSessionFee());
    }

    @Test
    @DisplayName("Trainer - equals and hashCode work correctly (Lombok @Data)")
    void trainer_equalsAndHashCode() {
        Trainer t1 = new Trainer(1L, "Alice", "alice@gym.com", "1234567890", "Yoga", 5, "ACTIVE", null);
        Trainer t2 = new Trainer(1L, "Alice", "alice@gym.com", "1234567890", "Yoga", 5, "ACTIVE", null);

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    @DisplayName("Trainer - toString returns non-null string")
    void trainer_toString_isNotNull() {
        Trainer trainer = new Trainer(1L, "Alice", "alice@gym.com", "1234", "Yoga", 5, "ACTIVE", null);
        assertNotNull(trainer.toString());
        assertTrue(trainer.toString().contains("Alice"));
    }

    @Test
    @DisplayName("Trainer - two trainers with different ids are not equal")
    void trainer_differentIds_notEqual() {
        Trainer t1 = new Trainer(1L, "Alice", "alice@gym.com", "1234567890", "Yoga", 5, "ACTIVE", null);
        Trainer t2 = new Trainer(2L, "Alice", "alice@gym.com", "1234567890", "Yoga", 5, "ACTIVE", null);

        assertNotEquals(t1, t2);
    }

    // ── TrainerSchedule entity ──────────────────────────────────

    @Test
    @DisplayName("TrainerSchedule - default constructor creates object with false available")
    void trainerSchedule_defaultConstructor() {
        TrainerSchedule schedule = new TrainerSchedule();
        assertNull(schedule.getId());
        assertNull(schedule.getTrainerId());
        assertNull(schedule.getSessionDate());
        assertNull(schedule.getStartTime());
        assertNull(schedule.getEndTime());
        assertEquals(0, schedule.getMaxCapacity());
        assertEquals(0, schedule.getBookedCount());
        assertFalse(schedule.isCancelled());
        assertFalse(schedule.isAvailable());
    }

    @Test
    @DisplayName("TrainerSchedule - all-args constructor sets all fields")
    void trainerSchedule_allArgsConstructor_setsAllFields() {
        LocalDate date = LocalDate.of(2026, 7, 15);
        TrainerSchedule schedule = new TrainerSchedule(2L, 1L, date, "10:00", "12:00", 20, 0, false, true);

        assertEquals(2L, schedule.getId());
        assertEquals(1L, schedule.getTrainerId());
        assertEquals(date, schedule.getSessionDate());
        assertEquals("10:00", schedule.getStartTime());
        assertEquals("12:00", schedule.getEndTime());
        assertEquals(20, schedule.getMaxCapacity());
        assertEquals(0, schedule.getBookedCount());
        assertFalse(schedule.isCancelled());
        assertTrue(schedule.isAvailable());
    }

    @Test
    @DisplayName("TrainerSchedule - setters update fields correctly")
    void trainerSchedule_setters_updateFields() {
        LocalDate date = LocalDate.of(2026, 8, 1);
        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setId(3L);
        schedule.setTrainerId(7L);
        schedule.setSessionDate(date);
        schedule.setStartTime("08:00");
        schedule.setEndTime("10:00");
        schedule.setMaxCapacity(12);
        schedule.setBookedCount(3);
        schedule.setCancelled(false);
        schedule.setAvailable(true);

        assertEquals(3L, schedule.getId());
        assertEquals(7L, schedule.getTrainerId());
        assertEquals(date, schedule.getSessionDate());
        assertEquals("08:00", schedule.getStartTime());
        assertEquals("10:00", schedule.getEndTime());
        assertEquals(12, schedule.getMaxCapacity());
        assertEquals(3, schedule.getBookedCount());
        assertTrue(schedule.isAvailable());
    }

    @Test
    @DisplayName("TrainerSchedule - marking unavailable works")
    void trainerSchedule_setUnavailable() {
        TrainerSchedule schedule = new TrainerSchedule();
        schedule.setAvailable(true);
        assertTrue(schedule.isAvailable());

        schedule.setAvailable(false);
        assertFalse(schedule.isAvailable());
    }

    @Test
    @DisplayName("TrainerSchedule - marking cancelled works")
    void trainerSchedule_setCancelled() {
        TrainerSchedule schedule = new TrainerSchedule();
        assertFalse(schedule.isCancelled());

        schedule.setCancelled(true);
        assertTrue(schedule.isCancelled());
    }

    @Test
    @DisplayName("TrainerSchedule - equals and hashCode work (Lombok @Data)")
    void trainerSchedule_equalsAndHashCode() {
        LocalDate date = LocalDate.of(2026, 7, 15);
        TrainerSchedule s1 = new TrainerSchedule(1L, 1L, date, "09:00", "11:00", 20, 0, false, true);
        TrainerSchedule s2 = new TrainerSchedule(1L, 1L, date, "09:00", "11:00", 20, 0, false, true);

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    @DisplayName("TrainerSchedule - toString returns non-null string")
    void trainerSchedule_toString_isNotNull() {
        LocalDate date = LocalDate.of(2026, 7, 15);
        TrainerSchedule schedule = new TrainerSchedule(1L, 1L, date, "09:00", "11:00", 20, 0, false, true);
        assertNotNull(schedule.toString());
        assertTrue(schedule.toString().contains("09:00"));
    }

    // ── Exception classes ───────────────────────────────────────

    @Test
    @DisplayName("TrainerNotFoundException - id constructor message contains id")
    void trainerNotFoundException_idConstructor() {
        TrainerNotFoundException ex = new TrainerNotFoundException(99L);
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("99"));
        assertTrue(ex.getMessage().toLowerCase().contains("trainer"));
    }

    @Test
    @DisplayName("TrainerNotFoundException - string constructor sets message")
    void trainerNotFoundException_stringConstructor() {
        TrainerNotFoundException ex = new TrainerNotFoundException("Trainer is unavailable");
        assertEquals("Trainer is unavailable", ex.getMessage());
    }

    @Test
    @DisplayName("TrainerNotFoundException - is a RuntimeException")
    void trainerNotFoundException_isRuntimeException() {
        TrainerNotFoundException ex = new TrainerNotFoundException(1L);
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("ScheduleNotFoundException - id constructor message contains id")
    void scheduleNotFoundException_idConstructor() {
        ScheduleNotFoundException ex = new ScheduleNotFoundException(55L);
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("55"));
    }

    @Test
    @DisplayName("ScheduleNotFoundException - string constructor sets message")
    void scheduleNotFoundException_stringConstructor() {
        ScheduleNotFoundException ex = new ScheduleNotFoundException("No slot available");
        assertEquals("No slot available", ex.getMessage());
    }

    @Test
    @DisplayName("ScheduleNotFoundException - is a RuntimeException")
    void scheduleNotFoundException_isRuntimeException() {
        ScheduleNotFoundException ex = new ScheduleNotFoundException(1L);
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("AccessDeniedException - role constructor includes role in message")
    void accessDeniedException_roleConstructor() {
        AccessDeniedException ex = new AccessDeniedException("GUEST");
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("GUEST"));
    }

    @Test
    @DisplayName("AccessDeniedException - no-arg constructor has default message")
    void accessDeniedException_noArgConstructor() {
        AccessDeniedException ex = new AccessDeniedException();
        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().isEmpty());
    }

    @Test
    @DisplayName("AccessDeniedException - is a RuntimeException")
    void accessDeniedException_isRuntimeException() {
        AccessDeniedException ex = new AccessDeniedException("X");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
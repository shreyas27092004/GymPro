package com.gympro.booking.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FreeSessionCheckResultAndTrainerDtoTest {

    // ── FreeSessionCheckResult ─────────────────────────────────────────

    @Test
    @DisplayName("FreeSessionCheckResult no-args constructor defaults to false/zero/null")
    void freeSessionCheckResult_noArgsConstructor_defaults() {
        FreeSessionCheckResult result = new FreeSessionCheckResult();

        assertThat(result.isFree()).isFalse();
        assertThat(result.getRemainingFreeSessions()).isZero();
        assertThat(result.getTotalFreeSessions()).isZero();
        assertThat(result.getSubscriptionId()).isNull();
    }

    @Test
    @DisplayName("FreeSessionCheckResult all-args constructor sets all fields")
    void freeSessionCheckResult_allArgsConstructor_setsFields() {
        FreeSessionCheckResult result = new FreeSessionCheckResult(true, 3, 5, 99L);

        assertThat(result.isFree()).isTrue();
        assertThat(result.getRemainingFreeSessions()).isEqualTo(3);
        assertThat(result.getTotalFreeSessions()).isEqualTo(5);
        assertThat(result.getSubscriptionId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("FreeSessionCheckResult setters update fields")
    void freeSessionCheckResult_setters_updateFields() {
        FreeSessionCheckResult result = new FreeSessionCheckResult();

        result.setFree(true);
        result.setRemainingFreeSessions(-1);
        result.setTotalFreeSessions(-1);
        result.setSubscriptionId(10L);

        assertThat(result.isFree()).isTrue();
        assertThat(result.getRemainingFreeSessions()).isEqualTo(-1);
        assertThat(result.getTotalFreeSessions()).isEqualTo(-1);
        assertThat(result.getSubscriptionId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("FreeSessionCheckResult equals/hashCode consistent for equal values")
    void freeSessionCheckResult_equalsAndHashCode() {
        FreeSessionCheckResult a = new FreeSessionCheckResult(true, 1, 1, 1L);
        FreeSessionCheckResult b = new FreeSessionCheckResult(true, 1, 1, 1L);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("FreeSessionCheckResult toString includes field values")
    void freeSessionCheckResult_toString_includesFields() {
        FreeSessionCheckResult result = new FreeSessionCheckResult(true, 2, 3, 4L);

        assertThat(result.toString()).contains("true", "2", "3", "4");
    }

    // ── TrainerDto ────────────────────────────────────────────────────

    @Test
    @DisplayName("TrainerDto no-args constructor creates an empty instance")
    void trainerDto_noArgsConstructor_createsEmptyInstance() {
        TrainerDto dto = new TrainerDto();

        assertThat(dto.getId()).isNull();
        assertThat(dto.getSessionFee()).isNull();
    }

    @Test
    @DisplayName("TrainerDto setters and getters work for all fields")
    void trainerDto_settersAndGetters_workForAllFields() {
        TrainerDto dto = new TrainerDto();

        dto.setId(1L);
        dto.setName("Coach Amy");
        dto.setEmail("amy@gym.com");
        dto.setSpecialization("Strength");
        dto.setStatus("ACTIVE");
        dto.setSessionFee(new BigDecimal("500.00"));

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Coach Amy");
        assertThat(dto.getEmail()).isEqualTo("amy@gym.com");
        assertThat(dto.getSpecialization()).isEqualTo("Strength");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
        assertThat(dto.getSessionFee()).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("TrainerDto equals/hashCode consistent for equal values")
    void trainerDto_equalsAndHashCode() {
        TrainerDto a = new TrainerDto();
        a.setId(1L);
        a.setSessionFee(new BigDecimal("500.00"));

        TrainerDto b = new TrainerDto();
        b.setId(1L);
        b.setSessionFee(new BigDecimal("500.00"));

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("TrainerDto toString includes field values")
    void trainerDto_toString_includesFields() {
        TrainerDto dto = new TrainerDto();
        dto.setId(1L);
        dto.setName("Coach Amy");

        assertThat(dto.toString()).contains("Coach Amy");
    }
}

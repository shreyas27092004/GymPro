package com.gympro.trainer;

import com.gympro.trainer.entity.Trainer;
import com.gympro.trainer.entity.TrainerSchedule;
import com.gympro.trainer.exception.ScheduleNotFoundException;
import com.gympro.trainer.exception.TrainerNotFoundException;
import com.gympro.trainer.repository.TrainerRepository;
import com.gympro.trainer.repository.TrainerScheduleRepository;
import com.gympro.trainer.service.TrainerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainerService Unit Tests")
class TrainerServiceTest {

    @Mock
    private TrainerRepository trainerRepository;

    @Mock
    private TrainerScheduleRepository scheduleRepository;

    @InjectMocks
    private TrainerService trainerService;

    private Trainer sampleTrainer;
    private TrainerSchedule sampleSchedule;

    @BeforeEach
    void setUp() {
        sampleTrainer = new Trainer();
        sampleTrainer.setId(1L);
        sampleTrainer.setName("John Doe");
        sampleTrainer.setEmail("john@gym.com");
        sampleTrainer.setPhone("9876543210");
        sampleTrainer.setSpecialization("Yoga");
        sampleTrainer.setExperienceYears(5);
        sampleTrainer.setStatus("ACTIVE");

        sampleSchedule = new TrainerSchedule();
        sampleSchedule.setId(1L);
        sampleSchedule.setTrainerId(1L);
        sampleSchedule.setDayOfWeek("MON");
        sampleSchedule.setStartTime("09:00");
        sampleSchedule.setEndTime("11:00");
        sampleSchedule.setAvailable(true);
    }

    // ── createTrainer ──────────────────────────────────────────

    @Test
    @DisplayName("createTrainer - should save trainer with ACTIVE status")
    void createTrainer_shouldSetStatusActiveAndSave() {
        Trainer input = new Trainer();
        input.setName("Jane Smith");
        input.setEmail("jane@gym.com");

        when(trainerRepository.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.createTrainer(input);

        assertEquals("ACTIVE", result.getStatus());
        verify(trainerRepository, times(1)).save(input);
    }

    @Test
    @DisplayName("createTrainer - should preserve an explicitly-supplied status (e.g. PENDING)")
    void createTrainer_shouldPreserveExplicitStatus() {
        Trainer input = new Trainer();
        input.setName("New Trainer");
        input.setEmail("new@gym.com");
        input.setStatus("PENDING");

        when(trainerRepository.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.createTrainer(input);

        assertEquals("PENDING", result.getStatus());
        verify(trainerRepository, times(1)).save(input);
    }

    // ── Trainer approval workflow ────────────────────────────────

    @Test
    @DisplayName("getPendingTrainers - should return trainers with status=PENDING")
    void getPendingTrainers_shouldReturnPendingList() {
        Trainer pending = new Trainer();
        pending.setId(5L);
        pending.setStatus("PENDING");
        when(trainerRepository.findByStatus("PENDING")).thenReturn(Arrays.asList(pending));

        List<Trainer> result = trainerService.getPendingTrainers();

        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).getStatus());
        verify(trainerRepository, times(1)).findByStatus("PENDING");
    }

    @Test
    @DisplayName("approveTrainer - should set status to ACTIVE and save")
    void approveTrainer_shouldSetStatusActive() {
        sampleTrainer.setStatus("PENDING");
        when(trainerRepository.findById(1L)).thenReturn(Optional.of(sampleTrainer));
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.approveTrainer(1L);

        assertEquals("ACTIVE", result.getStatus());
        verify(trainerRepository, times(1)).save(sampleTrainer);
    }

    @Test
    @DisplayName("approveTrainer - should throw when trainer does not exist")
    void approveTrainer_shouldThrow_whenNotFound() {
        when(trainerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TrainerNotFoundException.class, () -> trainerService.approveTrainer(99L));
        verify(trainerRepository, never()).save(any(Trainer.class));
    }

    @Test
    @DisplayName("rejectTrainer - should set status to REJECTED and save")
    void rejectTrainer_shouldSetStatusRejected() {
        sampleTrainer.setStatus("PENDING");
        when(trainerRepository.findById(1L)).thenReturn(Optional.of(sampleTrainer));
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.rejectTrainer(1L);

        assertEquals("REJECTED", result.getStatus());
        verify(trainerRepository, times(1)).save(sampleTrainer);
    }

    @Test
    @DisplayName("rejectTrainer - should throw when trainer does not exist")
    void rejectTrainer_shouldThrow_whenNotFound() {
        when(trainerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TrainerNotFoundException.class, () -> trainerService.rejectTrainer(99L));
        verify(trainerRepository, never()).save(any(Trainer.class));
    }

    @Test
    @DisplayName("getAllTrainers - should return all trainers")
    void getAllTrainers_shouldReturnList() {
        when(trainerRepository.findAll()).thenReturn(Arrays.asList(sampleTrainer));

        List<Trainer> result = trainerService.getAllTrainers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
        verify(trainerRepository, times(1)).findAll();
    }

    // ── getTrainerById ─────────────────────────────────────────

    @Test
    @DisplayName("getTrainerById - should return trainer when found")
    void getTrainerById_shouldReturnTrainer_whenExists() {
        when(trainerRepository.findById(1L)).thenReturn(Optional.of(sampleTrainer));

        Trainer result = trainerService.getTrainerById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John Doe", result.getName());
    }

    @Test
    @DisplayName("getTrainerById - should throw TrainerNotFoundException when not found")
    void getTrainerById_shouldThrowException_whenNotFound() {
        when(trainerRepository.findById(99L)).thenReturn(Optional.empty());

        TrainerNotFoundException ex = assertThrows(
            TrainerNotFoundException.class,
            () -> trainerService.getTrainerById(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
    }

    // ── getBySpecialization ────────────────────────────────────

    @Test
    @DisplayName("getBySpecialization - should return matching trainers")
    void getBySpecialization_shouldReturnFilteredList() {
        when(trainerRepository.findBySpecialization("Yoga")).thenReturn(Arrays.asList(sampleTrainer));

        List<Trainer> result = trainerService.getBySpecialization("Yoga");

        assertEquals(1, result.size());
        assertEquals("Yoga", result.get(0).getSpecialization());
    }

    // ── updateTrainer ──────────────────────────────────────────

    @Test
    @DisplayName("updateTrainer - should update fields and save")
    void updateTrainer_shouldUpdateFieldsAndSave() {
        Trainer updated = new Trainer();
        updated.setName("John Updated");
        updated.setPhone("1112223333");
        updated.setSpecialization("Cardio");
        updated.setExperienceYears(7);

        when(trainerRepository.findById(1L)).thenReturn(Optional.of(sampleTrainer));
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.updateTrainer(1L, updated);

        assertEquals("John Updated", result.getName());
        assertEquals("Cardio", result.getSpecialization());
        assertEquals(7, result.getExperienceYears());
        verify(trainerRepository, times(1)).save(any(Trainer.class));
    }

    @Test
    @DisplayName("updateTrainer - should throw TrainerNotFoundException for missing id")
    void updateTrainer_shouldThrow_whenTrainerNotFound() {
        when(trainerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TrainerNotFoundException.class,
            () -> trainerService.updateTrainer(99L, new Trainer()));
    }

    // ── deleteTrainer ──────────────────────────────────────────

    @Test
    @DisplayName("deleteTrainer - should set status INACTIVE")
    void deleteTrainer_shouldSetInactiveStatus() {
        when(trainerRepository.findById(1L)).thenReturn(Optional.of(sampleTrainer));
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = trainerService.deleteTrainer(1L);

        assertEquals("INACTIVE", sampleTrainer.getStatus());
        assertTrue(result.contains("deactivated"));
        verify(trainerRepository, times(1)).save(sampleTrainer);
    }

    // ── addSchedule ────────────────────────────────────────────

    @Test
    @DisplayName("addSchedule - should save schedule with available=true")
    void addSchedule_shouldSetAvailableTrueAndSave() {
        TrainerSchedule input = new TrainerSchedule();
        input.setTrainerId(1L);
        input.setDayOfWeek("TUE");
        input.setStartTime("14:00");
        input.setEndTime("16:00");

        when(scheduleRepository.save(any(TrainerSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        TrainerSchedule result = trainerService.addSchedule(input);

        assertTrue(result.isAvailable());
        verify(scheduleRepository, times(1)).save(input);
    }

    // ── getSchedule ────────────────────────────────────────────

    @Test
    @DisplayName("getSchedule - should return all slots for a trainer")
    void getSchedule_shouldReturnAllSlots() {
        when(scheduleRepository.findByTrainerId(1L)).thenReturn(Arrays.asList(sampleSchedule));

        List<TrainerSchedule> result = trainerService.getSchedule(1L);

        assertEquals(1, result.size());
        assertEquals("MON", result.get(0).getDayOfWeek());
    }

    // ── getAvailableSlots ──────────────────────────────────────

    @Test
    @DisplayName("getAvailableSlots - should return only available slots")
    void getAvailableSlots_shouldReturnAvailableOnly() {
        when(scheduleRepository.findByTrainerIdAndAvailable(1L, true))
            .thenReturn(Arrays.asList(sampleSchedule));

        List<TrainerSchedule> result = trainerService.getAvailableSlots(1L);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isAvailable());
    }

    // ── markSlotBooked ─────────────────────────────────────────

    @Test
    @DisplayName("markSlotBooked - should set available=false")
    void markSlotBooked_shouldMarkAsUnavailable() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(sampleSchedule));
        when(scheduleRepository.save(any(TrainerSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        trainerService.markSlotBooked(1L);

        assertFalse(sampleSchedule.isAvailable());
        verify(scheduleRepository, times(1)).save(sampleSchedule);
    }

    // ── getByEmail ─────────────────────────────────────────────

    @Test
    @DisplayName("getByEmail - should return trainer when found")
    void getByEmail_shouldReturnTrainer_whenExists() {
        when(trainerRepository.findByEmail("john@gym.com")).thenReturn(Optional.of(sampleTrainer));

        Trainer result = trainerService.getByEmail("john@gym.com");

        assertNotNull(result);
        assertEquals("john@gym.com", result.getEmail());
        verify(trainerRepository, times(1)).findByEmail("john@gym.com");
    }

    @Test
    @DisplayName("getByEmail - should throw TrainerNotFoundException when not found")
    void getByEmail_shouldThrow_whenNotFound() {
        when(trainerRepository.findByEmail("missing@gym.com")).thenReturn(Optional.empty());

        assertThrows(TrainerNotFoundException.class,
            () -> trainerService.getByEmail("missing@gym.com"));
    }

    // ── updateTrainer – status branch ─────────────────────────────

    @Test
    @DisplayName("updateTrainer - should overwrite status when provided and non-blank")
    void updateTrainer_shouldOverwriteStatus_whenProvided() {
        Trainer updated = new Trainer();
        updated.setName("John Updated");
        updated.setStatus("INACTIVE");

        when(trainerRepository.findById(1L)).thenReturn(Optional.of(sampleTrainer));
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.updateTrainer(1L, updated);

        assertEquals("INACTIVE", result.getStatus());
    }

    @Test
    @DisplayName("updateTrainer - should keep existing status when updated status is blank")
    void updateTrainer_shouldKeepStatus_whenBlank() {
        Trainer updated = new Trainer();
        updated.setName("John Updated");
        updated.setStatus("   ");

        when(trainerRepository.findById(1L)).thenReturn(Optional.of(sampleTrainer));
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.updateTrainer(1L, updated);

        assertEquals("ACTIVE", result.getStatus());
    }

    // ── updateTrainer – session fee branch ────────────────────────

    @Test
    @DisplayName("updateTrainer - should persist a valid session fee")
    void updateTrainer_shouldPersistValidSessionFee() {
        Trainer updated = new Trainer();
        updated.setName("John Updated");
        updated.setSessionFee(new BigDecimal("600.00"));

        when(trainerRepository.findById(1L)).thenReturn(Optional.of(sampleTrainer));
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.updateTrainer(1L, updated);

        assertEquals(new BigDecimal("600.00"), result.getSessionFee());
    }

    @Test
    @DisplayName("updateTrainer - should throw when session fee is zero")
    void updateTrainer_shouldThrow_whenSessionFeeIsZero() {
        Trainer updated = new Trainer();
        updated.setSessionFee(BigDecimal.ZERO);

        when(trainerRepository.findById(1L)).thenReturn(Optional.of(sampleTrainer));

        assertThrows(IllegalArgumentException.class,
            () -> trainerService.updateTrainer(1L, updated));
        verify(trainerRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTrainer - should throw when session fee is negative")
    void updateTrainer_shouldThrow_whenSessionFeeIsNegative() {
        Trainer updated = new Trainer();
        updated.setSessionFee(new BigDecimal("-10.00"));

        when(trainerRepository.findById(1L)).thenReturn(Optional.of(sampleTrainer));

        assertThrows(IllegalArgumentException.class,
            () -> trainerService.updateTrainer(1L, updated));
    }

    @Test
    @DisplayName("updateTrainer - should not touch session fee when null")
    void updateTrainer_shouldSkipSessionFee_whenNull() {
        Trainer updated = new Trainer();
        updated.setName("John Updated");
        updated.setSessionFee(null);

        when(trainerRepository.findById(1L)).thenReturn(Optional.of(sampleTrainer));
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.updateTrainer(1L, updated);

        assertNull(result.getSessionFee());
    }

    // ── updateSessionFee ───────────────────────────────────────────

    @Test
    @DisplayName("updateSessionFee - should update fee when valid and trainer exists")
    void updateSessionFee_shouldUpdateFee_whenValid() {
        when(trainerRepository.findById(1L)).thenReturn(Optional.of(sampleTrainer));
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.updateSessionFee(1L, new BigDecimal("450.00"));

        assertEquals(new BigDecimal("450.00"), result.getSessionFee());
        verify(trainerRepository, times(1)).save(sampleTrainer);
    }

    @Test
    @DisplayName("updateSessionFee - should throw when fee is null")
    void updateSessionFee_shouldThrow_whenFeeIsNull() {
        assertThrows(IllegalArgumentException.class,
            () -> trainerService.updateSessionFee(1L, null));
        verify(trainerRepository, never()).findById(any());
    }

    @Test
    @DisplayName("updateSessionFee - should throw when fee is zero")
    void updateSessionFee_shouldThrow_whenFeeIsZero() {
        assertThrows(IllegalArgumentException.class,
            () -> trainerService.updateSessionFee(1L, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("updateSessionFee - should throw when fee is negative")
    void updateSessionFee_shouldThrow_whenFeeIsNegative() {
        assertThrows(IllegalArgumentException.class,
            () -> trainerService.updateSessionFee(1L, new BigDecimal("-5")));
    }

    @Test
    @DisplayName("updateSessionFee - should throw TrainerNotFoundException when trainer missing")
    void updateSessionFee_shouldThrow_whenTrainerNotFound() {
        when(trainerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TrainerNotFoundException.class,
            () -> trainerService.updateSessionFee(99L, new BigDecimal("100")));
    }

    // ── deleteSchedule ─────────────────────────────────────────────

    @Test
    @DisplayName("deleteSchedule - should delete an available slot")
    void deleteSchedule_shouldDelete_whenAvailable() {
        sampleSchedule.setAvailable(true);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(sampleSchedule));

        String result = trainerService.deleteSchedule(1L);

        assertTrue(result.contains("deleted"));
        verify(scheduleRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteSchedule - should throw IllegalStateException when slot is booked")
    void deleteSchedule_shouldThrow_whenBooked() {
        sampleSchedule.setAvailable(false);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(sampleSchedule));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> trainerService.deleteSchedule(1L));

        assertTrue(ex.getMessage().contains("booked"));
        verify(scheduleRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteSchedule - should throw ScheduleNotFoundException when slot missing")
    void deleteSchedule_shouldThrow_whenNotFound() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ScheduleNotFoundException.class,
            () -> trainerService.deleteSchedule(99L));
    }
}

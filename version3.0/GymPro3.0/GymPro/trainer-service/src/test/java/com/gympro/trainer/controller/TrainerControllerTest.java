package com.gympro.trainer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gympro.trainer.entity.Trainer;
import com.gympro.trainer.entity.TrainerSchedule;
import com.gympro.trainer.exception.AccessDeniedException;
import com.gympro.trainer.exception.GlobalExceptionHandler;
import com.gympro.trainer.service.TrainerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainerController Unit Tests")
class TrainerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TrainerService trainerService;

    @InjectMocks
    private TrainerController trainerController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Trainer sampleTrainer;
    private TrainerSchedule sampleSchedule;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(trainerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

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

    // ── POST /trainers ──────────────────────────────────────────

    @Test
    @DisplayName("POST /trainers - ADMIN role should create trainer successfully")
    void createTrainer_asAdmin_shouldReturn200() throws Exception {
        when(trainerService.createTrainer(any(Trainer.class))).thenReturn(sampleTrainer);

        mockMvc.perform(post("/trainers")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTrainer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@gym.com"));

        verify(trainerService, times(1)).createTrainer(any(Trainer.class));
    }

    @Test
    @DisplayName("POST /trainers - non-ADMIN role should throw AccessDeniedException → 403")
    void createTrainer_asNonAdmin_shouldReturn403() throws Exception {
        // Controller throws AccessDeniedException before calling service when role != ADMIN
        mockMvc.perform(post("/trainers")
                        .header("X-User-Role", "MEMBER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTrainer)))
                .andExpect(status().isForbidden());

        verify(trainerService, never()).createTrainer(any());
    }

    @Test
    @DisplayName("POST /trainers - TRAINER role should return 403")
    void createTrainer_asTrainer_shouldReturn403() throws Exception {
        mockMvc.perform(post("/trainers")
                        .header("X-User-Role", "TRAINER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTrainer)))
                .andExpect(status().isForbidden());
    }

    // ── GET /trainers ───────────────────────────────────────────

    @Test
    @DisplayName("GET /trainers - should return list of all trainers")
    void getAllTrainers_shouldReturnList() throws Exception {
        List<Trainer> trainers = Arrays.asList(sampleTrainer);
        when(trainerService.getAllTrainers()).thenReturn(trainers);

        mockMvc.perform(get("/trainers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("John Doe"));

        verify(trainerService, times(1)).getAllTrainers();
    }

    @Test
    @DisplayName("GET /trainers - should return empty list when no trainers exist")
    void getAllTrainers_shouldReturnEmptyList() throws Exception {
        when(trainerService.getAllTrainers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/trainers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /trainers/{id} ──────────────────────────────────────

    @Test
    @DisplayName("GET /trainers/{id} - should return trainer by id")
    void getById_shouldReturnTrainer() throws Exception {
        when(trainerService.getTrainerById(1L)).thenReturn(sampleTrainer);

        mockMvc.perform(get("/trainers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.specialization").value("Yoga"));
    }

    // ── GET /trainers/specialization/{spec} ─────────────────────

    @Test
    @DisplayName("GET /trainers/specialization/{spec} - should return matching trainers")
    void getBySpec_shouldReturnFilteredList() throws Exception {
        when(trainerService.getBySpecialization("Yoga")).thenReturn(Arrays.asList(sampleTrainer));

        mockMvc.perform(get("/trainers/specialization/Yoga"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].specialization").value("Yoga"));
    }

    @Test
    @DisplayName("GET /trainers/specialization/{spec} - no match returns empty list")
    void getBySpec_noMatch_shouldReturnEmpty() throws Exception {
        when(trainerService.getBySpecialization("Boxing")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/trainers/specialization/Boxing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── PUT /trainers/{id} ──────────────────────────────────────

    @Test
    @DisplayName("PUT /trainers/{id} - should update trainer and return updated")
    void updateTrainer_shouldReturnUpdatedTrainer() throws Exception {
        Trainer updated = new Trainer();
        updated.setId(1L);
        updated.setName("Jane Updated");
        updated.setSpecialization("Cardio");
        updated.setExperienceYears(8);

        when(trainerService.updateTrainer(eq(1L), any(Trainer.class))).thenReturn(updated);

        mockMvc.perform(put("/trainers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Updated"))
                .andExpect(jsonPath("$.specialization").value("Cardio"));
    }

    // ── DELETE /trainers/{id} ───────────────────────────────────

    @Test
    @DisplayName("DELETE /trainers/{id} - ADMIN role should deactivate trainer")
    void deleteTrainer_asAdmin_shouldReturnSuccessMessage() throws Exception {
        when(trainerService.deleteTrainer(1L)).thenReturn("Trainer deactivated ✅");

        mockMvc.perform(delete("/trainers/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().string("Trainer deactivated ✅"));
    }

    @Test
    @DisplayName("DELETE /trainers/{id} - MEMBER role should return 403")
    void deleteTrainer_asMember_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/trainers/1")
                        .header("X-User-Role", "MEMBER"))
                .andExpect(status().isForbidden());

        verify(trainerService, never()).deleteTrainer(anyLong());
    }

    @Test
    @DisplayName("DELETE /trainers/{id} - TRAINER role should return 403")
    void deleteTrainer_asTrainer_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/trainers/1")
                        .header("X-User-Role", "TRAINER"))
                .andExpect(status().isForbidden());

        verify(trainerService, never()).deleteTrainer(anyLong());
    }

    // ── POST /trainers/schedule ─────────────────────────────────

    @Test
    @DisplayName("POST /trainers/schedule - TRAINER role should add schedule")
    void addSchedule_asTrainer_shouldReturnSchedule() throws Exception {
        when(trainerService.addSchedule(any(TrainerSchedule.class))).thenReturn(sampleSchedule);

        mockMvc.perform(post("/trainers/schedule")
                        .header("X-User-Role", "TRAINER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleSchedule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek").value("MON"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("POST /trainers/schedule - ADMIN role should add schedule")
    void addSchedule_asAdmin_shouldReturnSchedule() throws Exception {
        when(trainerService.addSchedule(any(TrainerSchedule.class))).thenReturn(sampleSchedule);

        mockMvc.perform(post("/trainers/schedule")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleSchedule)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /trainers/schedule - MEMBER role should return 403")
    void addSchedule_asMember_shouldReturn403() throws Exception {
        mockMvc.perform(post("/trainers/schedule")
                        .header("X-User-Role", "MEMBER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleSchedule)))
                .andExpect(status().isForbidden());

        verify(trainerService, never()).addSchedule(any());
    }

    // ── GET /trainers/{id}/schedule ────────────────────────────

    @Test
    @DisplayName("GET /trainers/{id}/schedule - should return all schedule slots")
    void getSchedule_shouldReturnScheduleList() throws Exception {
        when(trainerService.getSchedule(1L)).thenReturn(Arrays.asList(sampleSchedule));

        mockMvc.perform(get("/trainers/1/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].startTime").value("09:00"));
    }

    // ── GET /trainers/{id}/available-slots ─────────────────────

    @Test
    @DisplayName("GET /trainers/{id}/available-slots - should return only available slots")
    void getAvailableSlots_shouldReturnOnlyAvailable() throws Exception {
        when(trainerService.getAvailableSlots(1L)).thenReturn(Arrays.asList(sampleSchedule));

        mockMvc.perform(get("/trainers/1/available-slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    // ── PUT /trainers/schedule/{scheduleId}/book ────────────────

    @Test
    @DisplayName("PUT /trainers/schedule/{scheduleId}/book - should mark slot as booked")
    void markBooked_shouldReturnSuccessMessage() throws Exception {
        doNothing().when(trainerService).markSlotBooked(1L);

        mockMvc.perform(put("/trainers/schedule/1/book"))
                .andExpect(status().isOk())
                .andExpect(content().string("Slot marked as booked ✅"));

        verify(trainerService, times(1)).markSlotBooked(1L);
    }

    // ── GET /trainers/by-email/{email} ──────────────────────────

    @Test
    @DisplayName("GET /trainers/by-email/{email} - should return trainer when found")
    void getByEmail_shouldReturnTrainer() throws Exception {
        when(trainerService.getByEmail("john@gym.com")).thenReturn(sampleTrainer);

        mockMvc.perform(get("/trainers/by-email/john@gym.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@gym.com"));
    }

    @Test
    @DisplayName("GET /trainers/by-email/{email} - should return 404 when not found")
    void getByEmail_shouldReturn404_whenNotFound() throws Exception {
        when(trainerService.getByEmail("missing@gym.com"))
                .thenThrow(new com.gympro.trainer.exception.TrainerNotFoundException("Trainer not found with email: missing@gym.com"));

        mockMvc.perform(get("/trainers/by-email/missing@gym.com"))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /trainers/{id}/session-fee ────────────────────────

    @Test
    @DisplayName("PATCH /trainers/{id}/session-fee - TRAINER role should update fee")
    void updateSessionFee_asTrainer_shouldReturn200() throws Exception {
        Trainer updated = new Trainer();
        updated.setId(1L);
        updated.setSessionFee(new java.math.BigDecimal("500.00"));
        when(trainerService.updateSessionFee(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(patch("/trainers/1/session-fee")
                        .header("X-User-Role", "TRAINER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionFee\": 500.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionFee").value(500.00));
    }

    @Test
    @DisplayName("PATCH /trainers/{id}/session-fee - ADMIN role should update fee")
    void updateSessionFee_asAdmin_shouldReturn200() throws Exception {
        Trainer updated = new Trainer();
        updated.setId(1L);
        updated.setSessionFee(new java.math.BigDecimal("500.00"));
        when(trainerService.updateSessionFee(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(patch("/trainers/1/session-fee")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionFee\": 500.00}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /trainers/{id}/session-fee - MEMBER role should return 403")
    void updateSessionFee_asMember_shouldReturn403() throws Exception {
        mockMvc.perform(patch("/trainers/1/session-fee")
                        .header("X-User-Role", "MEMBER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionFee\": 500.00}"))
                .andExpect(status().isForbidden());

        verify(trainerService, never()).updateSessionFee(anyLong(), any());
    }

    @Test
    @DisplayName("PATCH /trainers/{id}/session-fee - missing sessionFee field should return 400")
    void updateSessionFee_missingFeeField_shouldReturn400() throws Exception {
        mockMvc.perform(patch("/trainers/1/session-fee")
                        .header("X-User-Role", "TRAINER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(trainerService, never()).updateSessionFee(anyLong(), any());
    }

    // ── DELETE /trainers/schedule/{scheduleId} ──────────────────

    @Test
    @DisplayName("DELETE /trainers/schedule/{scheduleId} - TRAINER role should delete slot")
    void deleteSchedule_asTrainer_shouldReturn200() throws Exception {
        when(trainerService.deleteSchedule(1L)).thenReturn("Schedule slot deleted ✅");

        mockMvc.perform(delete("/trainers/schedule/1")
                        .header("X-User-Role", "TRAINER"))
                .andExpect(status().isOk())
                .andExpect(content().string("Schedule slot deleted ✅"));
    }

    @Test
    @DisplayName("DELETE /trainers/schedule/{scheduleId} - ADMIN role should delete slot")
    void deleteSchedule_asAdmin_shouldReturn200() throws Exception {
        when(trainerService.deleteSchedule(1L)).thenReturn("Schedule slot deleted ✅");

        mockMvc.perform(delete("/trainers/schedule/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /trainers/schedule/{scheduleId} - MEMBER role should return 403")
    void deleteSchedule_asMember_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/trainers/schedule/1")
                        .header("X-User-Role", "MEMBER"))
                .andExpect(status().isForbidden());

        verify(trainerService, never()).deleteSchedule(anyLong());
    }

    @Test
    @DisplayName("DELETE /trainers/schedule/{scheduleId} - booked slot should return 409")
    void deleteSchedule_bookedSlot_shouldReturn409() throws Exception {
        when(trainerService.deleteSchedule(1L))
                .thenThrow(new IllegalStateException("Cannot delete a booked slot. The slot #1 is already assigned to a session."));

        mockMvc.perform(delete("/trainers/schedule/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isConflict());
    }

    // ── GET /trainers/test ──────────────────────────────────────

    @Test
    @DisplayName("GET /trainers/test - should return health check message")
    void test_shouldReturnHealthMessage() throws Exception {
        mockMvc.perform(get("/trainers/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Trainer Service Working ✅"));
    }
}

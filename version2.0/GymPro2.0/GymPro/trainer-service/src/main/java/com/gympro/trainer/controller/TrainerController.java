package com.gympro.trainer.controller;

import com.gympro.trainer.entity.Trainer;
import com.gympro.trainer.entity.TrainerSchedule;
import com.gympro.trainer.exception.AccessDeniedException;
import com.gympro.trainer.service.TrainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// ✅ Role access:
//   POST /trainers             → ADMIN
//   GET  /trainers             → ALL authenticated
//   PUT  /trainers/{id}        → ADMIN or TRAINER (own)
//   DELETE /trainers/{id}      → ADMIN
//   POST /trainers/schedule    → TRAINER
//   GET  /trainers/{id}/schedule → ALL authenticated
//   PATCH /trainers/{id}/session-fee → TRAINER or ADMIN  [NEW]
@RestController
@RequestMapping("/trainers")
public class TrainerController {

    @Autowired
    private TrainerService service;

    @PostMapping
    public Trainer create(@RequestBody Trainer trainer,
                          @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new AccessDeniedException(role);
        return service.createTrainer(trainer);
    }

    @GetMapping
    public List<Trainer> getAll() {
        return service.getAllTrainers();
    }

    @GetMapping("/{id}")
    public Trainer getById(@PathVariable Long id) {
        return service.getTrainerById(id);
    }

    @GetMapping("/by-email/{email}")
    public Trainer getByEmail(@PathVariable String email) {
        return service.getByEmail(email);
    }

    @GetMapping("/specialization/{spec}")
    public List<Trainer> getBySpec(@PathVariable String spec) {
        return service.getBySpecialization(spec);
    }

    @PutMapping("/{id}")
    public Trainer update(@PathVariable Long id, @RequestBody Trainer trainer) {
        return service.updateTrainer(id, trainer);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id,
                         @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new AccessDeniedException(role);
        return service.deleteTrainer(id);
    }

    // ── NEW: Update session fee (TRAINER or ADMIN) ──────────────────────
    /**
     * PATCH /trainers/{id}/session-fee
     * Body: { "sessionFee": 500.00 }
     * Allows a trainer to set/update their per-session fee.
     */
    @PatchMapping("/{id}/session-fee")
    public Trainer updateSessionFee(@PathVariable Long id,
                                    @RequestBody Map<String, BigDecimal> body,
                                    @RequestHeader("X-User-Role") String role) {
        if (!"TRAINER".equals(role) && !"ADMIN".equals(role))
            throw new AccessDeniedException(role);
        BigDecimal fee = body.get("sessionFee");
        if (fee == null) {
            throw new IllegalArgumentException("sessionFee field is required in request body");
        }
        return service.updateSessionFee(id, fee);
    }

    // ── Schedule endpoints ──────────────────────────────────

    @PostMapping("/schedule")
    public TrainerSchedule addSchedule(@RequestBody TrainerSchedule schedule,
                                       @RequestHeader("X-User-Role") String role) {
        if (!"TRAINER".equals(role) && !"ADMIN".equals(role))
            throw new AccessDeniedException(role);
        return service.addSchedule(schedule);
    }

    @GetMapping("/{id}/schedule")
    public List<TrainerSchedule> getSchedule(@PathVariable Long id) {
        return service.getSchedule(id);
    }

    @GetMapping("/{id}/available-slots")
    public List<TrainerSchedule> getAvailable(@PathVariable Long id) {
        return service.getAvailableSlots(id);
    }

    @PutMapping("/schedule/{scheduleId}/book")
    public String markBooked(@PathVariable Long scheduleId) {
        service.markSlotBooked(scheduleId);
        return "Slot marked as booked ✅";
    }

    // DELETE a schedule slot — only allowed if it has NOT been booked
    @DeleteMapping("/schedule/{scheduleId}")
    public String deleteSchedule(@PathVariable Long scheduleId,
                                 @RequestHeader("X-User-Role") String role) {
        if (!"TRAINER".equals(role) && !"ADMIN".equals(role))
            throw new AccessDeniedException(role);
        return service.deleteSchedule(scheduleId);
    }

    @GetMapping("/test")
    public String test() { return "Trainer Service Working ✅"; }
}

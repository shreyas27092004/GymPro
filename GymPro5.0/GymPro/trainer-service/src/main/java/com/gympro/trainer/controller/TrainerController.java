package com.gympro.trainer.controller;

import jakarta.validation.Valid;
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
    public Trainer create(@Valid @RequestBody Trainer trainer,
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

    // ── NEW: Trainer approval workflow (ADMIN only) ──────────────────────
    /**
     * GET /trainers/pending
     * Lists trainers with status=PENDING, i.e. self-registered but not yet
     * approved by an admin. Used to power the admin "approvals" queue.
     */
    @GetMapping("/pending")
    public List<Trainer> getPending(@RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new AccessDeniedException(role);
        return service.getPendingTrainers();
    }

    /**
     * PATCH /trainers/{id}/approve
     * Moves a PENDING trainer to ACTIVE — they can now log in and are
     * visible to members for booking.
     */
    @PatchMapping("/{id}/approve")
    public Trainer approve(@PathVariable Long id, @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new AccessDeniedException(role);
        return service.approveTrainer(id);
    }

    /**
     * PATCH /trainers/{id}/reject
     * Moves a PENDING trainer to REJECTED — they remain unable to log in.
     */
    @PatchMapping("/{id}/reject")
    public Trainer reject(@PathVariable Long id, @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new AccessDeniedException(role);
        return service.rejectTrainer(id);
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
    public Trainer update(@PathVariable Long id, @Valid @RequestBody Trainer trainer) {
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

    // ── NEW: Fetch a single session slot ─────────────────────────────────
    // Used internally by booking-service (Feign) to validate capacity,
    // cancellation, and expiry before creating a booking.
    @GetMapping("/schedule/{scheduleId}")
    public TrainerSchedule getScheduleById(@PathVariable Long scheduleId) {
        return service.getScheduleById(scheduleId);
    }

    // Called by booking-service when a booking is created. Enforces max
    // capacity and rejects bookings on a cancelled session.
    @PutMapping("/schedule/{scheduleId}/book")
    public String markBooked(@PathVariable Long scheduleId) {
        service.markSlotBooked(scheduleId);
        return "Slot marked as booked ✅";
    }

    // ── NEW: Called by booking-service when a booking is cancelled, to
    // free up a capacity slot on the session.
    @PutMapping("/schedule/{scheduleId}/unbook")
    public String unbook(@PathVariable Long scheduleId) {
        service.unbookSlot(scheduleId);
        return "Slot released ✅";
    }

    // ── NEW: TRAINER/ADMIN cancels a scheduled session ───────────────────
    @PatchMapping("/schedule/{scheduleId}/cancel")
    public TrainerSchedule cancelSchedule(@PathVariable Long scheduleId,
                                          @RequestHeader("X-User-Role") String role) {
        if (!"TRAINER".equals(role) && !"ADMIN".equals(role))
            throw new AccessDeniedException(role);
        return service.cancelSchedule(scheduleId);
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

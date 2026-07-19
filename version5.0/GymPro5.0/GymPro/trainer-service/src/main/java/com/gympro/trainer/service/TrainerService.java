package com.gympro.trainer.service;

import com.gympro.trainer.exception.DuplicatePhoneException;
import com.gympro.trainer.entity.Trainer;
import com.gympro.trainer.entity.TrainerSchedule;
import com.gympro.trainer.exception.DuplicateScheduleException;
import com.gympro.trainer.exception.ScheduleNotFoundException;
import com.gympro.trainer.exception.TrainerNotFoundException;
import com.gympro.trainer.repository.TrainerRepository;
import com.gympro.trainer.repository.TrainerScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class TrainerService {

    @Autowired private TrainerRepository repo;
    @Autowired private TrainerScheduleRepository scheduleRepo;

    // ── Trainer CRUD ──────────────────────────────────────────

    public Trainer createTrainer(Trainer trainer) {
        rejectDuplicatePhone(trainer.getPhone(), null);
        // Respect an explicitly-supplied status (e.g. "PENDING" when auth-service
        // auto-creates this profile on trainer self-registration). Only default to
        // ACTIVE when the caller didn't specify one — this keeps the admin's
        // "Add Trainer" form (which always sends ACTIVE) working exactly as before.
        if (trainer.getStatus() == null || trainer.getStatus().isBlank()) {
            trainer.setStatus("ACTIVE");
        }
        return repo.save(trainer);
    }

    /** Trainers awaiting admin approval. */
    public List<Trainer> getPendingTrainers() {
        return repo.findByStatus("PENDING");
    }

    /** ADMIN approves a pending trainer — they can now log in and appear to members. */
    public Trainer approveTrainer(Long id) {
        Trainer t = getTrainerById(id);
        t.setStatus("ACTIVE");
        return repo.save(t);
    }

    /** ADMIN rejects a pending trainer — they remain unable to log in. */
    public Trainer rejectTrainer(Long id) {
        Trainer t = getTrainerById(id);
        t.setStatus("REJECTED");
        return repo.save(t);
    }

    public List<Trainer> getAllTrainers() {
        return repo.findAll();
    }

    public Trainer getTrainerById(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new TrainerNotFoundException(id));
    }

    public Trainer getByEmail(String email) {
        return repo.findByEmail(email)
            .orElseThrow(() -> new TrainerNotFoundException("Trainer not found with email: " + email));
    }

    public List<Trainer> getBySpecialization(String spec) {
        return repo.findBySpecialization(spec);
    }

    public Trainer updateTrainer(Long id, Trainer updated) {
        Trainer existing = getTrainerById(id);
        existing.setName(updated.getName());
        rejectDuplicatePhone(updated.getPhone(), id);
        existing.setPhone(updated.getPhone());
        existing.setSpecialization(updated.getSpecialization());
        existing.setExperienceYears(updated.getExperienceYears());
        // Persist status changes (ACTIVE / INACTIVE) — required for toggle to work
        if (updated.getStatus() != null && !updated.getStatus().isBlank()) {
            existing.setStatus(updated.getStatus());
        }
        // ── NEW: persist session fee if provided ─────────────────────────
        if (updated.getSessionFee() != null) {
            if (updated.getSessionFee().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Session fee must be greater than 0");
            }
            existing.setSessionFee(updated.getSessionFee());
        }
        return repo.save(existing);
    }
    /**
     * Rejects a phone number that already belongs to another trainer.
     * Format/blank rules are enforced separately via Bean Validation on the
     * incoming request; this checks uniqueness against persisted data, which
     * Bean Validation cannot do on its own.
     *
     * @param phone     the phone number being set
     * @param excludeId the id of the trainer currently being updated (null on create),
     *                  so a trainer is not flagged as a duplicate of itself
     */
    private void rejectDuplicatePhone(String phone, Long excludeId) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        repo.findByPhone(phone.trim())
            .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
            .ifPresent(existing -> {
                throw new DuplicatePhoneException(phone.trim());
            });
    }

    /**
     * NEW: Allows a trainer to update ONLY their session fee.
     * Used by the trainer dashboard — does not touch other fields.
     */
    public Trainer updateSessionFee(Long id, BigDecimal fee) {
        if (fee == null || fee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Session fee must be greater than 0");
        }
        Trainer existing = getTrainerById(id);
        existing.setSessionFee(fee);
        return repo.save(existing);
    }

    public String deleteTrainer(Long id) {
        Trainer t = getTrainerById(id);
        t.setStatus("INACTIVE");
        repo.save(t);
        return "Trainer deactivated ✅";
    }

    // ── Schedule (calendar-based sessions) ───────────────────────────────

    /**
     * Creates a new dated session for a trainer.
     *
     * Validations enforced (Problems #1, #2, #3, #5):
     *  - sessionDate is required, must be a real calendar date, and cannot be in the past.
     *  - startTime/endTime are required and endTime must be after startTime.
     *  - maxCapacity must be greater than 0.
     *  - No two sessions may exist for the same trainer on the same date at the
     *    same start time (checked here, and backed by a DB unique constraint
     *    as a race-condition safety net).
     */
    public TrainerSchedule addSchedule(TrainerSchedule schedule) {
        validateNewSchedule(schedule);

        scheduleRepo.findByTrainerIdAndSessionDateAndStartTime(
                schedule.getTrainerId(), schedule.getSessionDate(), schedule.getStartTime())
            .ifPresent(existing -> {
                throw duplicateScheduleException(schedule);
            });

        schedule.setBookedCount(0);
        schedule.setCancelled(false);
        schedule.setAvailable(true);

        try {
            return scheduleRepo.save(schedule);
        } catch (DataIntegrityViolationException e) {
            // Safety net: guards against a concurrent request slipping past the
            // findBy... check above and hitting the DB unique constraint instead.
            throw duplicateScheduleException(schedule);
        }
    }

    private DuplicateScheduleException duplicateScheduleException(TrainerSchedule schedule) {
        return new DuplicateScheduleException(
            "Trainer #" + schedule.getTrainerId() + " already has a session scheduled on " +
            schedule.getSessionDate() + " at " + schedule.getStartTime() +
            ". Same trainer + same date + same time must be unique."
        );
    }

    private void validateNewSchedule(TrainerSchedule schedule) {
        if (schedule.getTrainerId() == null) {
            throw new IllegalArgumentException("trainerId is required");
        }
        if (schedule.getSessionDate() == null) {
            throw new IllegalArgumentException(
                "sessionDate is required — please choose a calendar date for the session (e.g. 2026-07-15)");
        }
        if (schedule.getSessionDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("sessionDate cannot be in the past");
        }
        if (schedule.getStartTime() == null || schedule.getStartTime().isBlank()) {
            throw new IllegalArgumentException("startTime is required");
        }
        if (schedule.getEndTime() == null || schedule.getEndTime().isBlank()) {
            throw new IllegalArgumentException("endTime is required");
        }

        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(schedule.getStartTime());
            end = LocalTime.parse(schedule.getEndTime());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("startTime/endTime must be in HH:mm format, e.g. 10:00");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        if (schedule.getMaxCapacity() <= 0) {
            throw new IllegalArgumentException("maxCapacity must be greater than 0");
        }
    }

    public List<TrainerSchedule> getSchedule(Long trainerId) {
        return scheduleRepo.findByTrainerId(trainerId);
    }

    public List<TrainerSchedule> getAvailableSlots(Long trainerId) {
        return scheduleRepo.findByTrainerIdAndAvailable(trainerId, true);
    }

    public TrainerSchedule getScheduleById(Long scheduleId) {
        return scheduleRepo.findById(scheduleId)
            .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
    }

    public String deleteSchedule(Long scheduleId) {
        TrainerSchedule slot = getScheduleById(scheduleId);
        if (slot.getBookedCount() > 0) {
            throw new IllegalStateException(
                "Cannot delete a slot with existing bookings. Slot #" + scheduleId +
                " currently has " + slot.getBookedCount() + " member(s) booked."
            );
        }
        scheduleRepo.deleteById(scheduleId);
        return "Schedule slot deleted ✅";
    }

    /**
     * Called by booking-service when a member successfully books this session.
     * Enforces the capacity limit and rejects bookings on a cancelled session
     * (Problems #3 and #4). Increments bookedCount and keeps `available` in sync.
     */
    public void markSlotBooked(Long scheduleId) {
        TrainerSchedule slot = getScheduleById(scheduleId);

        if (slot.isCancelled()) {
            throw new IllegalStateException(
                "Cannot book slot #" + scheduleId + " — this session has been cancelled by the trainer");
        }
        if (slot.getBookedCount() >= slot.getMaxCapacity()) {
            throw new IllegalStateException(
                "Cannot book slot #" + scheduleId + " — session is already at full capacity (" +
                slot.getMaxCapacity() + ")");
        }

        slot.setBookedCount(slot.getBookedCount() + 1);
        slot.setAvailable(slot.getBookedCount() < slot.getMaxCapacity());
        scheduleRepo.save(slot);
    }

    /**
     * Called by booking-service when a booking against this session is cancelled,
     * to free up a capacity slot.
     */
    public void unbookSlot(Long scheduleId) {
        TrainerSchedule slot = getScheduleById(scheduleId);

        if (slot.getBookedCount() > 0) {
            slot.setBookedCount(slot.getBookedCount() - 1);
        }
        if (!slot.isCancelled()) {
            slot.setAvailable(slot.getBookedCount() < slot.getMaxCapacity());
        }
        scheduleRepo.save(slot);
    }

    /**
     * TRAINER/ADMIN cancels a scheduled session outright (e.g. trainer is sick).
     * The session becomes unavailable for new bookings. Existing bookings are
     * the responsibility of booking-service to reconcile.
     */
    public TrainerSchedule cancelSchedule(Long scheduleId) {
        TrainerSchedule slot = getScheduleById(scheduleId);
        slot.setCancelled(true);
        slot.setAvailable(false);
        return scheduleRepo.save(slot);
    }
}

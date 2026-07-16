package com.gympro.trainer.service;

import com.gympro.trainer.entity.Trainer;
import com.gympro.trainer.entity.TrainerSchedule;
import com.gympro.trainer.exception.ScheduleNotFoundException;
import com.gympro.trainer.exception.TrainerNotFoundException;
import com.gympro.trainer.repository.TrainerRepository;
import com.gympro.trainer.repository.TrainerScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TrainerService {

    @Autowired private TrainerRepository repo;
    @Autowired private TrainerScheduleRepository scheduleRepo;

    // ── Trainer CRUD ──────────────────────────────────────────

    public Trainer createTrainer(Trainer trainer) {
        trainer.setStatus("ACTIVE");
        return repo.save(trainer);
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
            .orElseThrow(() -> new TrainerNotFoundException(0L));
    }

    public List<Trainer> getBySpecialization(String spec) {
        return repo.findBySpecialization(spec);
    }

    public Trainer updateTrainer(Long id, Trainer updated) {
        Trainer existing = getTrainerById(id);
        existing.setName(updated.getName());
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

    // ── Schedule ───────────────────────────────────────────────

    public TrainerSchedule addSchedule(TrainerSchedule schedule) {
        schedule.setAvailable(true);
        return scheduleRepo.save(schedule);
    }

    public List<TrainerSchedule> getSchedule(Long trainerId) {
        return scheduleRepo.findByTrainerId(trainerId);
    }

    public List<TrainerSchedule> getAvailableSlots(Long trainerId) {
        return scheduleRepo.findByTrainerIdAndAvailable(trainerId, true);
    }

    public String deleteSchedule(Long scheduleId) {
        TrainerSchedule slot = scheduleRepo.findById(scheduleId)
            .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
        if (!slot.isAvailable()) {
            throw new IllegalStateException(
                "Cannot delete a booked slot. The slot #" + scheduleId +
                " is already assigned to a session."
            );
        }
        scheduleRepo.deleteById(scheduleId);
        return "Schedule slot deleted ✅";
    }

    public void markSlotBooked(Long scheduleId) {
        TrainerSchedule slot = scheduleRepo.findById(scheduleId)
            .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
        slot.setAvailable(false);
        scheduleRepo.save(slot);
    }
}

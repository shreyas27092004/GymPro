package com.gympro.trainer.repository;

import com.gympro.trainer.entity.TrainerSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainerScheduleRepository extends JpaRepository<TrainerSchedule, Long> {

    List<TrainerSchedule> findByTrainerId(Long trainerId);

    List<TrainerSchedule> findByTrainerIdAndAvailable(Long trainerId, boolean available);

    /** Used to detect duplicate sessions: same trainer, same date, same start time. */
    Optional<TrainerSchedule> findByTrainerIdAndSessionDateAndStartTime(
        Long trainerId, LocalDate sessionDate, String startTime);
}

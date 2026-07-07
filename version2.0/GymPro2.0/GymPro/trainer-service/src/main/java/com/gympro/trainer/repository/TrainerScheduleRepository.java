package com.gympro.trainer.repository;

import com.gympro.trainer.entity.TrainerSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrainerScheduleRepository extends JpaRepository<TrainerSchedule, Long> {
    List<TrainerSchedule> findByTrainerId(Long trainerId);
    List<TrainerSchedule> findByTrainerIdAndAvailable(Long trainerId, boolean available);
}

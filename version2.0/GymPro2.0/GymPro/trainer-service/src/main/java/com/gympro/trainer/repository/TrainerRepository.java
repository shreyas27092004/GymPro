package com.gympro.trainer.repository;

import com.gympro.trainer.entity.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {
    List<Trainer> findBySpecialization(String specialization);
    List<Trainer> findByStatus(String status);
    Optional<Trainer> findByEmail(String email);
}

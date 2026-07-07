package com.gympro.trainer.entity;

import jakarta.persistence.*;
import lombok.*;

// ✅ When is a trainer available for sessions?
@Entity
@Table(name = "trainer_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainerSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long trainerId;
    private String dayOfWeek;    // MON, TUE, WED, THU, FRI, SAT, SUN
    private String startTime;    // e.g. "09:00"
    private String endTime;      // e.g. "11:00"
    private boolean available;   // true = slot open for booking
}

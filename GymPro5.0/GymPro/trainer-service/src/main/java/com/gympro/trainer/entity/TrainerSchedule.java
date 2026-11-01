package com.gympro.trainer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// ✅ A concrete, dated training session slot created by a trainer.
//
// Replaces the old weekly (dayOfWeek) model — a trainer now schedules a
// session on an actual calendar date (e.g. 15 July 2026, 10:00 AM) instead
// of a recurring "MON 10 AM" slot.
//
// A trainer cannot create two sessions for the same date + start time
// (enforced both here via a DB unique constraint and in TrainerService for
// a friendly error message).
@Entity
@Table(
    name = "trainer_schedules",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_trainer_date_starttime",
        columnNames = {"trainerId", "sessionDate", "startTime"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainerSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long trainerId;

    /** Actual calendar date of the session, e.g. 2026-07-15. */
    @Column(nullable = false)
    private LocalDate sessionDate;

    private String startTime;    // e.g. "10:00"
    private String endTime;      // e.g. "11:00"

    /**
     * Maximum number of members who may book this session.
     * e.g. Yoga = 20, Personal Training = 1, HIIT = 12.
     */
    @Column(nullable = false)
    private int maxCapacity;

    /** Number of members currently booked into this session. Maintained by TrainerService. */
    private int bookedCount;

    /** true once the trainer explicitly cancels this session. */
    private boolean cancelled;

    /**
     * Derived convenience flag: true = still open for booking
     * (not cancelled AND bookedCount &lt; maxCapacity). Kept in sync by
     * TrainerService whenever bookedCount/cancelled change, so clients that
     * only read this flag (e.g. the frontend) keep working.
     */
    private boolean available;
}

package com.gympro.booking.repository;

import com.gympro.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByMemberId(Long memberId);
    List<Booking> findByTrainerId(Long trainerId);
    List<Booking> findByStatus(String status);

    /** Used to reject duplicate bookings: same member, same schedule, still active. */
    boolean existsByMemberIdAndScheduleIdAndStatus(Long memberId, Long scheduleId, String status);

    /**
     * True if an active (non-cancelled) booking already exists for this
     * trainer's schedule slot on this date. Used to reject double-bookings
     * of the same slot by two different members.
     */
    boolean existsByTrainerIdAndScheduleIdAndBookingDateAndStatusNot(
        Long trainerId, Long scheduleId, LocalDate bookingDate, String status);
}

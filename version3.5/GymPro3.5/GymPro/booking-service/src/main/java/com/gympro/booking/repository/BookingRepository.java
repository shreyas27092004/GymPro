package com.gympro.booking.repository;

import com.gympro.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByMemberId(Long memberId);
    List<Booking> findByTrainerId(Long trainerId);
    List<Booking> findByStatus(String status);
}

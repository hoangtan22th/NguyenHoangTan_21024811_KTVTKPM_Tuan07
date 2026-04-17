package com.example.bookingservice.repository;

import com.example.bookingservice.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeatStatusRepository extends JpaRepository<SeatStatus, Long> {
    // Hàm này rất quan trọng để check trùng ghế
    Optional<SeatStatus> findByMovieIdAndSeatNumber(Long movieId, String seatNumber);
}
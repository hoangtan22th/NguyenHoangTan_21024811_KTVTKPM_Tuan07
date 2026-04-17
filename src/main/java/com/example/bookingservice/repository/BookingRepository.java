package com.example.bookingservice.repository;

import com.example.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b.seatNumber FROM Booking b WHERE b.movieId = :movieId AND b.status = 'SUCCESS'")
    List<String> findBookedSeatsByMovieId(@Param("movieId") Long movieId);
}
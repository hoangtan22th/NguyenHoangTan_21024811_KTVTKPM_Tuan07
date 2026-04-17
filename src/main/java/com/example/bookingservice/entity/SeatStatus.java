package com.example.bookingservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "seat_statuses")
@Data
public class SeatStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatState status;
}
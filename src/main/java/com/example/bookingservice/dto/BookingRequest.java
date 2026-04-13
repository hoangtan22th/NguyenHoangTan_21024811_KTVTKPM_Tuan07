package com.example.bookingservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BookingRequest {
    private Long userId;
    private Long movieId;
    private String seatNumber;
    private BigDecimal totalPrice;
}
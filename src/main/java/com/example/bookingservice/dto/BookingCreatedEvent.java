package com.example.bookingservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BookingCreatedEvent {
    private Long bookingId;
    private Long userId;
    private BigDecimal totalAmount; // Phải đặt đúng tên này để Payment Service đọc được
}
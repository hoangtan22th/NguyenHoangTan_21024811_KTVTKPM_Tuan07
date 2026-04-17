package com.example.bookingservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // Cực kỳ quan trọng để chống lỗi rớt data
public class PaymentEventMessage {
    private Long bookingId;
    private String status;
    private String message; // Hứng lý do lỗi từ Payment gửi về
}
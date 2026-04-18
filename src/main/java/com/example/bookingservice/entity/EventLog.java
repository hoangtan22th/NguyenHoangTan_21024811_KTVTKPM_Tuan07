package com.example.bookingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;    // ID của đơn hàng để dễ tra cứu
    private String eventType;  // Ví dụ: BOOKING_CREATED_SENT, PAYMENT_RECEIVED

    @Column(columnDefinition = "TEXT")
    private String payload;    // Lưu toàn bộ cục JSON đã bắn/nhận để đối soát

    @CreationTimestamp
    private LocalDateTime createdAt;
}
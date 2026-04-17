package com.example.bookingservice.service;

import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.entity.BookingStatus;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Booking Service - Core Service xử lý đặt vé [cite: 167]
 * Áp dụng kiến trúc Event-Driven Architecture [cite: 135]
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RabbitTemplate rabbitTemplate; // Dùng để publish event sang RabbitMQ [cite: 137]

    /**
     * Quy trình tạo Booking (Event-Driven chuẩn):
     * 1. Nhận thông tin từ Frontend/Gateway (Tin tưởng userId từ Token/Gateway truyền xuống)
     * 2. Lưu Booking với trạng thái PENDING vào MariaDB
     * 3. Publish event BOOKING_CREATED (Bất đồng bộ) cho Payment xử lý
     */
    @Transactional
    public Booking createBooking(BookingRequest request) {

        // Bước 1: TẠO ENTITY VÀ LƯU VÀO DATABASE
        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setMovieId(request.getMovieId());
        booking.setSeatNumber(request.getSeatNumber());
        booking.setTotalPrice(request.getTotalPrice());

        // Mặc định trạng thái là PENDING khi mới tạo để chờ Payment Service xử lý
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Đã lưu booking thành công vào MariaDB: ID = {}, Status = {}", savedBooking.getId(), savedBooking.getStatus());

        // Bước 2: PUBLISH EVENT BOOKING_CREATED LÊN RABBITMQ [cite: 139, 172]
        // KHÔNG xử lý thanh toán trực tiếp tại đây theo yêu cầu kiến trúc [cite: 173]
        log.info("Chuẩn bị publish event BOOKING_CREATED lên RabbitMQ exchange: {}", RabbitMQConfig.EXCHANGE);

        try {
            // Gửi toàn bộ object savedBooking (đã được parse sang JSON nhờ cấu hình MessageConverter)
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY_BOOKING,
                    savedBooking
            );
            log.info("Đã bắn event thành công cho Booking ID: {}", savedBooking.getId());
        } catch (Exception e) {
            log.error("Lỗi khi bắn event lên RabbitMQ: {}", e.getMessage());
            // Trong thực tế có thể dùng Dead Letter Queue [cite: 199] hoặc Outbox Pattern ở đây
        }

        // Trả kết quả về ngay cho người dùng mà không cần chờ thanh toán
        return savedBooking;
    }

    /**
     * Lấy danh sách toàn bộ đơn đặt vé [cite: 170]
     */
    public List<Booking> getAllBookings() {
        log.info("Đang lấy danh sách toàn bộ đơn hàng từ database...");
        return bookingRepository.findAll();
    }
}
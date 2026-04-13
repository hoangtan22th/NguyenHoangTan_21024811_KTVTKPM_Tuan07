package com.example.bookingservice.service;

import com.example.bookingservice.client.UserClient; // Interface Feign để gọi User Service
import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.dto.UserDTO;
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
    private final UserClient userClient; // Dùng Feign Client để validate user từ User Service

    /**
     * Quy trình tạo Booking:
     * 1. Validate User (Đồng bộ qua REST)
     * 2. Lưu Booking PENDING vào MariaDB
     * 3. Publish event BOOKING_CREATED (Bất đồng bộ)
     */
    @Transactional
    public Booking createBooking(BookingRequest request) {

//        // Bước 1: KIỂM TRA USER THẬT (Gọi sang User Service qua Feign)
//        log.info("Đang kiểm tra thông tin User ID: {} từ User Service...", request.getUserId());
//        try {
//            UserDTO user = userClient.getUserById(request.getUserId());
//            log.info("Xác nhận User tồn tại: {}", user.getUsername());
//        } catch (Exception e) {
//            log.error("Lỗi: Không tìm thấy User ID {} hoặc User Service không hoạt động", request.getUserId());
//            throw new RuntimeException("User không tồn tại hoặc lỗi kết nối hệ thống. Không thể đặt vé!");
//        }

        // Bước 2: TẠO ENTITY VÀ LƯU VÀO DATABASE
        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setMovieId(request.getMovieId());
        booking.setSeatNumber(request.getSeatNumber());
        booking.setTotalPrice(request.getTotalPrice());

        // Mặc định trạng thái là PENDING khi mới tạo
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Đã lưu booking thành công vào MariaDB: ID = {}, Status = {}", savedBooking.getId(), savedBooking.getStatus());

//        // Bước 3: PUBLISH EVENT BOOKING_CREATED LÊN RABBITMQ [cite: 139, 172]
//        // KHÔNG xử lý thanh toán trực tiếp tại đây theo yêu cầu kiến trúc [cite: 173]
//        log.info("Chuẩn bị publish event BOOKING_CREATED lên RabbitMQ exchange: {}", RabbitMQConfig.EXCHANGE);
//
//        try {
//            rabbitTemplate.convertAndSend(
//                    RabbitMQConfig.EXCHANGE,
//                    RabbitMQConfig.ROUTING_KEY_BOOKING,
//                    savedBooking
//            );
//            log.info("Đã bắn event thành công cho Booking ID: {}", savedBooking.getId());
//        } catch (Exception e) {
//            log.error("Lỗi khi bắn event lên RabbitMQ: {}", e.getMessage());
//            // Tùy chọn: Có thể throw exception để rollback DB hoặc log để xử lý sau (Dead Letter Queue) [cite: 199]
//        }

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
//package com.example.bookingservice.service;
//
//import com.example.bookingservice.dto.BookingCreatedEvent;
//import com.example.bookingservice.dto.BookingRequest;
//import com.example.bookingservice.dto.PaymentEventMessage;
//import com.example.bookingservice.entity.Booking;
//import com.example.bookingservice.entity.BookingStatus;
//import com.example.bookingservice.repository.BookingRepository;
//import com.example.bookingservice.config.RabbitMQConfig;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
///**
// * Booking Service - Core Service xử lý đặt vé
// * Áp dụng kiến trúc Event-Driven Architecture
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class BookingService {
//
//    private final BookingRepository bookingRepository;
//    private final RabbitTemplate rabbitTemplate;
//
//    @Transactional
//    public Booking createBooking(BookingRequest request) {
//        log.info("============== BẮT ĐẦU TẠO BOOKING ==============");
//        log.debug("[DEBUG] Dữ liệu request nhận được: {}", request);
//
//        // Bước 1: TẠO ENTITY VÀ LƯU VÀO DATABASE
//        Booking booking = new Booking();
//        booking.setUserId(request.getUserId());
//        booking.setMovieId(request.getMovieId());
//        booking.setSeatNumber(request.getSeatNumber());
//        booking.setTotalPrice(request.getTotalPrice());
//        booking.setStatus(BookingStatus.PENDING);
//
//        Booking savedBooking = bookingRepository.save(booking);
//        log.info("[1] Đã lưu MariaDB -> Booking ID: {}, Status: {}", savedBooking.getId(), savedBooking.getStatus());
//
//        // Bước 2: PUBLISH EVENT LÊN RABBITMQ (CHUẨN DTO)
//        try {
//            log.debug("[DEBUG] Bắt đầu convert Entity sang DTO BookingCreatedEvent");
//            BookingCreatedEvent event = new BookingCreatedEvent();
//            event.setBookingId(savedBooking.getId());
//            event.setUserId(savedBooking.getUserId());
//            event.setTotalAmount(savedBooking.getTotalPrice()); // Lấy totalPrice gán vào totalAmount
//
//            log.debug("[DEBUG] Dữ liệu chuẩn bị gửi lên RabbitMQ: {}", event);
//
//            rabbitTemplate.convertAndSend(
//                    RabbitMQConfig.EXCHANGE,
//                    RabbitMQConfig.ROUTING_KEY_BOOKING,
//                    event // LƯU Ý: Gửi Event DTO, tuyệt đối KHÔNG gửi savedBooking
//            );
//            log.info("[2] Đã bắn event BOOKING_CREATED thành công lên RabbitMQ!");
//        } catch (Exception e) {
//            log.error("[LỖI] Không thể bắn event lên RabbitMQ: {}", e.getMessage(), e);
//        }
//
//        log.info("============== KẾT THÚC TẠO BOOKING ==============\n");
//        return savedBooking;
//    }
//
//    public List<Booking> getAllBookings() {
//        return bookingRepository.findAll();
//    }
//
//    public Booking getBookingById(Long id) {
//        return bookingRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
//    }
//
//    // =====================================================================
//    // KHU VỰC LẮNG NGHE KẾT QUẢ THANH TOÁN
//    // =====================================================================
//
//    @RabbitListener(queues = {RabbitMQConfig.QUEUE_BOOKING_UPDATE_SUCCESS, RabbitMQConfig.QUEUE_BOOKING_UPDATE_FAILED})
//    @Transactional
//    public void handlePaymentResult(PaymentEventMessage paymentResult) {
//        log.info("<<<<<<<<<<<<<< NHẬN EVENT TỪ PAYMENT <<<<<<<<<<<<<<");
//        log.info("<<<<<<<<<<<<<< NHẬN EVENT TỪ PAYMENT <<<<<<<<<<<<<<");
//        log.debug("[DEBUG] Cục JSON Payment gửi về ép kiểu thành công: {}", paymentResult);
//
//        // 1. Tìm đơn hàng
//        Booking existingBooking = bookingRepository.findById(paymentResult.getBookingId())
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + paymentResult.getBookingId()));
//
//        log.debug("[DEBUG] Trạng thái đơn hàng trước khi update: {}", existingBooking.getStatus());
//
//        // 2. Cập nhật trạng thái
//        if ("SUCCESS".equalsIgnoreCase(paymentResult.getStatus()) || "COMPLETED".equalsIgnoreCase(paymentResult.getStatus())) {
//            existingBooking.setStatus(BookingStatus.SUCCESS);
//            log.info("✅ Thanh toán OK! Đã update Booking ID {} thành SUCCESS", existingBooking.getId());
//        } else {
//            existingBooking.setStatus(BookingStatus.FAILED);
//            log.error("❌ Thanh toán LỖI! Lý do: {}. Đã update Booking ID {} thành FAILED", paymentResult.getMessage(), existingBooking.getId());
//        }
//
//        // 3. Lưu xuống DB
//        bookingRepository.save(existingBooking);
//        log.info("<<<<<<<<<<<<<< KẾT THÚC CẬP NHẬT DB <<<<<<<<<<<<<<\n");
//    }
//
//}

package com.example.bookingservice.service;

import com.example.bookingservice.dto.BookingCreatedEvent;
import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.dto.PaymentEventMessage;
import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.entity.BookingStatus;
import com.example.bookingservice.entity.SeatState;
import com.example.bookingservice.entity.SeatStatus;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.repository.SeatStatusRepository;
import com.example.bookingservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Booking Service - Core Service xử lý đặt vé
 * Áp dụng kiến trúc Event-Driven Architecture + Cơ chế Giữ ghế (Seat Holding)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatStatusRepository seatStatusRepository; // Bổ sung Repository quản lý ghế
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Booking createBooking(BookingRequest request) {
        log.info("============== BẮT ĐẦU TẠO BOOKING ==============");
        log.debug("[DEBUG] Dữ liệu request nhận được: {}", request);

        // --- BƯỚC 1: KIỂM TRA TRẠNG THÁI GHẾ (CHỐNG DOUBLE-BOOKING) ---
        Optional<SeatStatus> existingSeat = seatStatusRepository.findByMovieIdAndSeatNumber(
                request.getMovieId(), request.getSeatNumber());

        if (existingSeat.isPresent()) {
            SeatState currentState = existingSeat.get().getStatus();
            if (currentState == SeatState.BOOKED || currentState == SeatState.HOLDING) {
                log.warn("❌ Ghế {} đã bị chiếm. Trạng thái hiện tại: {}", request.getSeatNumber(), currentState);
                throw new RuntimeException("Xin lỗi, ghế " + request.getSeatNumber() + " đã có người đặt hoặc đang chờ thanh toán!");
            }
        }

        // --- BƯỚC 2: ĐÁNH DẤU GIỮ CHỖ (HOLDING) ---
        SeatStatus seatStatus = existingSeat.orElse(new SeatStatus());
        seatStatus.setMovieId(request.getMovieId());
        seatStatus.setSeatNumber(request.getSeatNumber());
        seatStatus.setStatus(SeatState.HOLDING);
        seatStatusRepository.save(seatStatus);
        log.info("[1] Đã giữ chỗ (HOLDING) thành công cho ghế {}", request.getSeatNumber());

        // --- BƯỚC 3: TẠO ENTITY VÀ LƯU BOOKING VÀO DATABASE ---
        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setMovieId(request.getMovieId());
        booking.setSeatNumber(request.getSeatNumber());
        booking.setTotalPrice(request.getTotalPrice());
        booking.setStatus(BookingStatus.PENDING); // Mặc định chờ thanh toán

        Booking savedBooking = bookingRepository.save(booking);
        log.info("[2] Đã lưu MariaDB -> Booking ID: {}, Status: {}", savedBooking.getId(), savedBooking.getStatus());

        // --- BƯỚC 4: PUBLISH EVENT LÊN RABBITMQ (CHUẨN DTO) ---
        try {
            log.debug("[DEBUG] Bắt đầu convert Entity sang DTO BookingCreatedEvent");
            BookingCreatedEvent event = new BookingCreatedEvent();
            event.setBookingId(savedBooking.getId());
            event.setUserId(savedBooking.getUserId());
            event.setTotalAmount(savedBooking.getTotalPrice()); // Ép đúng tên biến Payment cần

            log.debug("[DEBUG] Dữ liệu chuẩn bị gửi lên RabbitMQ: {}", event);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY_BOOKING,
                    event
            );
            log.info("[3] Đã bắn event BOOKING_CREATED thành công lên RabbitMQ!");
        } catch (Exception e) {
            log.error("[LỖI] Không thể bắn event lên RabbitMQ: {}", e.getMessage(), e);
        }

        log.info("============== KẾT THÚC TẠO BOOKING ==============\n");
        return savedBooking;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
    }

    // =====================================================================
    // KHU VỰC LẮNG NGHE KẾT QUẢ THANH TOÁN (NHẢ GHẾ HOẶC CHỐT GHẾ)
    // =====================================================================

    @RabbitListener(queues = {RabbitMQConfig.QUEUE_BOOKING_UPDATE_SUCCESS, RabbitMQConfig.QUEUE_BOOKING_UPDATE_FAILED})
    @Transactional
    public void handlePaymentResult(PaymentEventMessage paymentResult) {

//        demo gây lỗi
//        if (true) throw new RuntimeException("TESTING DLQ: Cố tình gây lỗi để xem tin nhắn bay vào sọt rác!");
        log.info("<<<<<<<<<<<<<< NHẬN EVENT TỪ PAYMENT <<<<<<<<<<<<<<");
        log.debug("[DEBUG] Cục JSON Payment gửi về ép kiểu thành công: {}", paymentResult);

        // 1. Tìm đơn hàng
        Booking existingBooking = bookingRepository.findById(paymentResult.getBookingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID: " + paymentResult.getBookingId()));

        log.debug("[DEBUG] Trạng thái đơn hàng trước khi update: {}", existingBooking.getStatus());

        // 2. Tìm cái ghế đang giữ của đơn hàng này
        SeatStatus seatStatus = seatStatusRepository.findByMovieIdAndSeatNumber(
                        existingBooking.getMovieId(), existingBooking.getSeatNumber())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin ghế trong hệ thống!"));

        // 3. Cập nhật trạng thái Booking và Seat
        if ("SUCCESS".equalsIgnoreCase(paymentResult.getStatus()) || "COMPLETED".equalsIgnoreCase(paymentResult.getStatus())) {
            existingBooking.setStatus(BookingStatus.SUCCESS);

            // Tiền về -> Chốt cứng ghế
            seatStatus.setStatus(SeatState.BOOKED);
            seatStatusRepository.save(seatStatus);

            log.info("✅ Thanh toán OK! Đã update Booking ID {} thành SUCCESS và chốt ghế BOOKED!", existingBooking.getId());
        } else {
            existingBooking.setStatus(BookingStatus.FAILED);

            // Tiền xịt -> Xóa luôn trạng thái ghế để nhả cho người khác mua
            seatStatusRepository.delete(seatStatus);

            log.error("❌ Thanh toán LỖI! Lý do: {}. Đã update Booking ID {} thành FAILED và nhả ghế {}.",
                    paymentResult.getMessage(), existingBooking.getId(), existingBooking.getSeatNumber());
        }

        // 4. Lưu Booking xuống DB
        bookingRepository.save(existingBooking);
        log.info("<<<<<<<<<<<<<< KẾT THÚC CẬP NHẬT DB <<<<<<<<<<<<<<\n");
    }
    public List<String> getBookedSeatsForMovie(Long movieId) {
        log.info("Đang lấy danh sách ghế đã đặt cho phim ID: {}", movieId);
        return bookingRepository.findBookedSeatsByMovieId(movieId);
    }
}
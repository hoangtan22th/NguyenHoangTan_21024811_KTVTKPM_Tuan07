package com.example.bookingservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "movie_ticket_exchange";

    // --- QUEUE KHI TẠO BOOKING ---
    public static final String QUEUE_BOOKING = "booking_created_queue";
    public static final String ROUTING_KEY_BOOKING = "booking.created";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue queue() {
        return new Queue(QUEUE_BOOKING);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_BOOKING);
    }

    // --- CẤU HÌNH CONVERTER CHUẨN ---
    @Bean
    public MessageConverter converter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

    // =========================================================================
    // KHU VỰC NHẬN KẾT QUẢ TỪ PAYMENT & DEAD LETTER QUEUE (BONUS)
    // =========================================================================

    public static final String QUEUE_DLQ = "dlq_booking_queue"; // Hàng đợi sọt rác (Bonus 1)

    // Tách 2 queue riêng biệt cho Booking để không giành giật với Notification
    public static final String QUEUE_BOOKING_UPDATE_SUCCESS = "booking_payment_success_queue";
    public static final String QUEUE_BOOKING_UPDATE_FAILED = "booking_payment_failed_queue";

    public static final String ROUTING_KEY_PAYMENT_COMPLETED = "payment.completed";
    public static final String ROUTING_KEY_BOOKING_FAILED = "booking.failed";

    // 1. Tạo hàng đợi sọt rác
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(QUEUE_DLQ);
    }

    // 2. Tạo Queue nhận thành công (Nếu lỗi thì tự văng vào sọt rác)
    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(QUEUE_BOOKING_UPDATE_SUCCESS)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_DLQ)
                .build();
    }

    // 3. Tạo Queue nhận thất bại (Nếu lỗi thì tự văng vào sọt rác)
    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(QUEUE_BOOKING_UPDATE_FAILED)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_DLQ)
                .build();
    }

    // 4. Binding các Queue
    @Bean
    public Binding bindingPaymentCompleted(Queue paymentSuccessQueue, TopicExchange exchange) {
        return BindingBuilder.bind(paymentSuccessQueue).to(exchange).with(ROUTING_KEY_PAYMENT_COMPLETED);
    }

    @Bean
    public Binding bindingBookingFailed(Queue paymentFailedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(paymentFailedQueue).to(exchange).with(ROUTING_KEY_BOOKING_FAILED);
    }
}
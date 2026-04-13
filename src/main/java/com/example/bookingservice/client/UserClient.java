package com.example.bookingservice.client;

import com.example.bookingservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserClient {

    // Giả sử bên User Service viết là GET /users/{id}
    @GetMapping("/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
}
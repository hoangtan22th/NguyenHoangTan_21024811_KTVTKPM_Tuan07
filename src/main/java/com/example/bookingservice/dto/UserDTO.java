package com.example.bookingservice.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id; // Phải là id, không phải userId
    private String username;
    private String email;
    private String role;
}
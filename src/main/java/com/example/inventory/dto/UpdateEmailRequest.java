package com.example.inventory.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateEmailRequest {

    @Email(message = "Invalid email format")
    private String email;
}
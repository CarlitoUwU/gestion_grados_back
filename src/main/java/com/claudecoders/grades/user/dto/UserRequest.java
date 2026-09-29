package com.claudecoders.grades.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 255) String googleSubject,
        @NotBlank @Size(max = 50) String role,
        boolean active) {}

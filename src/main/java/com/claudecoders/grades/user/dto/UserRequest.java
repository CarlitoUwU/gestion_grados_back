package com.claudecoders.grades.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @Schema(minLength = 1) @NotBlank @Email @Size(min = 1, max = 150) String email,
        @Schema(minLength = 1) @NotBlank @Size(min = 1, max = 200) String fullName,
        @Size(max = 255) String googleSubject,
        @Schema(minLength = 1) @NotBlank @Size(min = 1, max = 50) String role,
        boolean active) {}

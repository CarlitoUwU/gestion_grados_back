package com.claudecoders.grades.teacher.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeacherRequest(
        @Size(max = 30) String documentNumber,
        @Schema(minLength = 1) @NotBlank @Size(min = 1, max = 200) String fullName,
        @Email @Size(max = 150) String email) {}

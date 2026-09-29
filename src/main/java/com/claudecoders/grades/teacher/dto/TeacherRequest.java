package com.claudecoders.grades.teacher.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeacherRequest(
        @Size(max = 30) String documentNumber,
        @NotBlank @Size(max = 200) String fullName,
        @Email @Size(max = 150) String email) {}

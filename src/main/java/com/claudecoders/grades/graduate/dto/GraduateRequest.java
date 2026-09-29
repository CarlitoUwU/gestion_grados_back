package com.claudecoders.grades.graduate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GraduateRequest(
        @Size(max = 30) String documentNumber,
        @NotBlank @Size(max = 150) String firstNames,
        @NotBlank @Size(max = 150) String lastNames,
        Long schoolId,
        @Size(max = 200) String academicProgram) {}

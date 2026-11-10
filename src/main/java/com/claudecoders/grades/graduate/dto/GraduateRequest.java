package com.claudecoders.grades.graduate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GraduateRequest(
        @Size(max = 30) String documentNumber,
        @Schema(minLength = 1) @NotBlank @Size(min = 1, max = 150) String firstNames,
        @Schema(minLength = 1) @NotBlank @Size(min = 1, max = 150) String lastNames,
        Long schoolId,
        @Size(max = 200) String academicProgram) {}

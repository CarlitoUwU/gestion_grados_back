package com.claudecoders.grades.school.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchoolRequest(
        @Schema(minLength = 1) @NotBlank @Size(min = 1, max = 200) String name) {}

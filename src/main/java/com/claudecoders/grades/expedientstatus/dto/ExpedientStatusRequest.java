package com.claudecoders.grades.expedientstatus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExpedientStatusRequest(
        @Schema(minLength = 1) @NotBlank @Size(min = 1, max = 100) String name, boolean active) {}

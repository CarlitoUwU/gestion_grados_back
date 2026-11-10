package com.claudecoders.grades.researchwork.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResearchWorkRequest(
        @NotNull Long expedientId,
        @Size(max = 50) String workType,
        @Schema(minLength = 1) @NotBlank @Size(min = 1) String title) {}

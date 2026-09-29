package com.claudecoders.grades.researchwork.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResearchWorkRequest(
        @NotNull Long expedientId, @Size(max = 50) String workType, @NotBlank String title) {}

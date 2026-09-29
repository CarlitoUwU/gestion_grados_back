package com.claudecoders.grades.expedientadvisor.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ExpedientAdvisorRequest(
        @NotNull Long expedientId,
        @NotNull Long teacherId,
        Long resolutionId,
        LocalDate assignedAt) {}

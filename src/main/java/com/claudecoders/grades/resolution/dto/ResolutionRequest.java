package com.claudecoders.grades.resolution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ResolutionRequest(
        @NotNull Long expedientId,
        @NotBlank @Size(max = 100) String number,
        LocalDate resolutionDate,
        @Size(max = 50) String resolutionType,
        String fileUrl) {}

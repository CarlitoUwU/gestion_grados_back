package com.claudecoders.grades.modality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DegreeModalityRequest(
        @Schema(minLength = 1) @NotBlank @Size(min = 1, max = 150) String name, boolean active) {}

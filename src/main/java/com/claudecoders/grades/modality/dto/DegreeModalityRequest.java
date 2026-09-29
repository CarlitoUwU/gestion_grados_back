package com.claudecoders.grades.modality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DegreeModalityRequest(@NotBlank @Size(max = 150) String name, boolean active) {}

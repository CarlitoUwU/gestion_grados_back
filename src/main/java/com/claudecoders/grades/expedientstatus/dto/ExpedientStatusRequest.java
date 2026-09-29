package com.claudecoders.grades.expedientstatus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExpedientStatusRequest(@NotBlank @Size(max = 100) String name, boolean active) {}

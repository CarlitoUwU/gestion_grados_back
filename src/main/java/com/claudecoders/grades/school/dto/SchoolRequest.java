package com.claudecoders.grades.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchoolRequest(@NotBlank @Size(max = 200) String name) {}

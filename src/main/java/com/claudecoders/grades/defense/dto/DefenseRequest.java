package com.claudecoders.grades.defense.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DefenseRequest(
        @NotNull Long expedientId, LocalDate defenseDate, @Size(max = 100) String result) {}

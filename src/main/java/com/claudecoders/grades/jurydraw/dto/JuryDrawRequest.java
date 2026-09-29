package com.claudecoders.grades.jurydraw.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record JuryDrawRequest(@NotNull Long expedientId, Long resolutionId, LocalDate drawDate) {}

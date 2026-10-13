package com.claudecoders.grades.expedient.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class ExpedientProcessRequests {
    private ExpedientProcessRequests() {}

    public record Status(@NotBlank @Size(max = 100) String status, String observations) {}

    public record Advisor(
            @NotNull @Positive Long teacherId,
            @NotBlank @Size(max = 100) String resolutionNumber,
            @NotNull LocalDate assignedAt) {}

    public record Jury(
            @NotBlank @Size(max = 100) String resolutionNumber,
            @NotNull @Size(min = 3, max = 3) List<@NotNull @Valid JuryMemberInput> members) {}

    public record JuryMemberInput(@NotNull @Positive Long teacherId, @NotBlank String role) {}

    public record Draw(
            @NotNull LocalDate drawDate,
            @NotBlank @Size(max = 100) String resolutionNumber,
            @NotEmpty List<@NotNull @Positive Long> teacherIds) {}

    public record Defense(
            @NotNull LocalDate defenseDate, @NotBlank @Size(max = 100) String result) {}
}

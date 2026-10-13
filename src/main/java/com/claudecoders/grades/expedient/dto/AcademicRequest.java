package com.claudecoders.grades.expedient.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Reemplazo total: una lista vacía elimina los elementos anteriores. */
public record AcademicRequest(
        @NotNull List<@NotNull @Valid ResolutionInput> resolutions,
        @Valid AdvisorInput advisor,
        List<@NotNull @Valid JuryInput> jury,
        @NotNull List<@NotNull @Valid DrawInput> draws,
        @Valid DefenseInput defense) {

    public record ResolutionInput(
            Long id,
            String clientId,
            @NotBlank @Size(max = 100) String number,
            LocalDate date,
            @Size(max = 50) String type) {}

    public record AdvisorInput(
            Long id, @NotNull Long teacherId, String resolutionClientId, LocalDate assignedAt) {}

    public record JuryInput(
            Long id, @NotNull Long teacherId, @NotNull String role, String resolutionClientId) {}

    public record DrawInput(
            Long id,
            @NotNull LocalDate date,
            String resolutionClientId,
            @NotNull List<@NotNull Long> teacherIds) {}

    public record DefenseInput(@NotNull LocalDate date, @Size(max = 100) String result) {}
}

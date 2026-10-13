package com.claudecoders.grades.expedient.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CompleteExpedientRequest(
        @NotBlank @Size(max = 100) String number,
        @NotNull LocalDate startDate,
        @NotNull Long modalityId,
        @NotNull Long statusId,
        @NotNull @Valid GraduateInput graduate,
        @NotNull @Valid ResearchInput research) {

    public record GraduateInput(
            @Size(max = 30) String documentNumber,
            @Size(max = 150) String firstNames,
            @Size(max = 150) String lastNames,
            @Size(max = 150) String fullName,
            @NotNull Long schoolId,
            @Size(max = 200) String academicProgram) {}

    public record ResearchInput(@NotBlank String title, @Size(max = 50) String workType) {}
}

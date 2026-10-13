package com.claudecoders.grades.expedient.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ExpedientWriteRequest(
        @NotBlank @Size(max = 100) String number,
        @NotNull LocalDate startDate,
        @Size(max = 150) String degree,
        @Positive Long modalityId,
        @Positive Long statusId,
        String observations,
        @NotNull @Valid GraduateInput graduate,
        @Valid ResearchInput research) {

    public record GraduateInput(
            @NotBlank @Size(max = 300) String fullName,
            @Size(max = 30) String documentNumber,
            @Size(max = 50) String studentCode,
            @Positive Long schoolId,
            @Size(max = 200) String academicProgram) {}

    public record ResearchInput(@NotBlank String title) {}
}

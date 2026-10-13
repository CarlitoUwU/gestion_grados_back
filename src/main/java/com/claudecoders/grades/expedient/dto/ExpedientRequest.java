package com.claudecoders.grades.expedient.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ExpedientRequest(
        @NotBlank @Size(max = 100) String number,
        @NotNull LocalDate startDate,
        @NotNull Long graduateId,
        Long modalityId,
        Long statusId,
        @Schema(description = "Obsoleto: se ignora y se obtiene del JWT") Long createdById,
        @Schema(description = "Obsoleto: se ignora y se obtiene del JWT") Long updatedById) {}

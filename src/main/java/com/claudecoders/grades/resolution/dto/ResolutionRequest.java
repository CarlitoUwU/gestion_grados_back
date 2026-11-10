package com.claudecoders.grades.resolution.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Datos de una resolución; los tres últimos campos admiten null")
public record ResolutionRequest(
        @Schema(description = "Expediente asociado", minimum = "1", example = "1")
                @NotNull
                @Positive
                Long expedientId,
        @Schema(
                        description =
                                "Número único dentro del expediente, sin distinguir mayúsculas",
                        minLength = 1,
                        maxLength = 100,
                        example = "RES-2026-001")
                @NotBlank
                @Size(min = 1, max = 100)
                String number,
        @Schema(description = "Fecha de la resolución", example = "2026-10-01", nullable = true)
                LocalDate resolutionDate,
        @Schema(
                        description = "Texto libre; no es un catálogo cerrado",
                        maxLength = 50,
                        example = "Asesor",
                        nullable = true)
                @Size(max = 50)
                String resolutionType,
        @Schema(
                        description =
                                "Enlace documental; use http o https para abrirlo desde el cliente",
                        example = "https://documentos.example/res-2026-001.pdf",
                        nullable = true)
                String fileUrl) {}

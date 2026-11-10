package com.claudecoders.grades.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(
        description = "Contrato común de errores",
        requiredProperties = {"timestamp", "status", "error", "message", "validationErrors"})
public record ApiError(
        @Schema(
                        description = "Fecha/hora del error en ISO 8601 UTC",
                        example = "2026-10-31T15:00:00Z",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Instant timestamp,
        @Schema(
                        description = "Código HTTP",
                        example = "400",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                int status,
        @Schema(example = "Bad Request", requiredMode = Schema.RequiredMode.REQUIRED) String error,
        @Schema(example = "Revisa los datos enviados", requiredMode = Schema.RequiredMode.REQUIRED)
                String message,
        @Schema(
                        description =
                                "Errores por campo; objeto vacío si no hay errores de validación",
                        example = "{\"number\":\"El número es obligatorio\"}",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Map<String, String> validationErrors) {}

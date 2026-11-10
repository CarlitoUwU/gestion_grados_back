package com.claudecoders.grades.dashboard;

import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "Indicadores del panel de inicio calculados con el corte de America/Lima")
public record DashboardResponse(
        @Schema(
                        description = "Todo el conjunto filtrado, no el tamaño de una página",
                        minimum = "0")
                long total,
        @Schema(
                        description =
                                "Registrado + En trámite + Observado (criterio PO pendiente de ratificar)",
                        minimum = "0")
                long active,
        @Schema(description = "Estados cuyo nombre contiene 'observ'", minimum = "0") long observed,
        @Schema(
                        description =
                                "Sustentados hasta el corte, con resultado informado y distinto de Pendiente",
                        minimum = "0")
                long defended,
        @Schema(description = "Nombre de estado a cantidad; la suma coincide con total")
                Map<String, Long> statusDistribution,
        @ArraySchema(
                        arraySchema = @Schema(description = "Hasta 5 sustentaciones pendientes"),
                        schema = @Schema(implementation = ExpedientApiModels.Summary.class))
                List<ExpedientApiModels.Summary> upcomingDefenses,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "Hasta 8 expedientes actualizados recientemente"),
                        schema = @Schema(implementation = ExpedientApiModels.Summary.class))
                List<ExpedientApiModels.Summary> recentExpedients,
        @Schema(
                        description = "Fecha de corte usada en todos los cálculos, en America/Lima",
                        example = "2026-10-31")
                LocalDate asOfDate,
        @Schema(description = "Zona horaria del corte", example = "America/Lima")
                String timeZone) {}

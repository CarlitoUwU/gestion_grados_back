package com.claudecoders.grades.expedient.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ExpedientApiModels {
    private ExpedientApiModels() {}

    @Schema(description = "Resumen de expediente usado en consultas, dashboard y detalle")
    public record Summary(
            @Schema(
                            description = "ID positivo",
                            minimum = "1",
                            requiredMode = Schema.RequiredMode.REQUIRED)
                    Long id,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String number,
            @Schema(
                            description = "Fecha de inicio YYYY-MM-DD",
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            example = "2026-01-15")
                    LocalDate startDate,
            @Schema(minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long graduateId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String graduateName,
            @Schema(nullable = true) String documentNumber,
            @Schema(minimum = "1", nullable = true) Long schoolId,
            @Schema(nullable = true) String schoolName,
            @Schema(nullable = true) String academicProgram,
            @Schema(minimum = "1", nullable = true) Long modalityId,
            @Schema(nullable = true) String modalityName,
            @Schema(minimum = "1", nullable = true) Long statusId,
            @Schema(nullable = true) String statusName,
            @Schema(nullable = true) String researchTitle,
            @Schema(description = "Fecha YYYY-MM-DD", nullable = true) LocalDate defenseDate,
            @Schema(nullable = true) String defenseResult,
            @Schema(
                            description =
                                    "Fecha/hora local sin offset en America/Lima; null si no se ha"
                                            + " registrado una actualización",
                            nullable = true,
                            example = "2026-10-31T15:00:00")
                    LocalDateTime updatedAt,
            @Schema(description = "Dato general; null si no está registrado", nullable = true)
                    String degree,
            @Schema(description = "Observaciones generales", nullable = true) String observations,
            @Schema(description = "Código de estudiante", nullable = true) String studentCode,
            @Schema(minimum = "1", nullable = true) Long researchWorkId,
            @Schema(nullable = true) String researchWorkType,
            @Schema(nullable = true) String graduateFirstNames,
            @Schema(nullable = true) String graduateLastNames,
            @Schema(nullable = true) String createdByName,
            @Schema(nullable = true) String updatedByName) {}

    @Schema(description = "Detalle completo del expediente")
    public record Detail(
            Summary summary,
            List<ResolutionItem> resolutions,
            AdvisorItem advisor,
            List<JuryItem> jury,
            List<DrawItem> draws,
            DefenseItem defense,
            @Schema(nullable = true) String degree,
            @Schema(nullable = true) String observations,
            GraduateItem graduate,
            ResearchItem research) {}

    @Schema(description = "Datos generales del graduando")
    public record GraduateItem(
            Long id,
            String fullName,
            String documentNumber,
            String studentCode,
            Long schoolId,
            String schoolName,
            String academicProgram) {}

    @Schema(description = "Trabajo de investigación asociado")
    public record ResearchItem(Long id, String title) {}

    public record PageResult(
            List<Summary> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last) {}

    public record ResolutionItem(
            Long id, String number, LocalDate date, String type, String fileUrl) {}

    public record AdvisorItem(
            Long id,
            Long teacherId,
            String teacherName,
            Long resolutionId,
            String resolutionNumber,
            LocalDate assignedAt) {}

    public record JuryItem(
            Long id,
            Long teacherId,
            String teacherName,
            String role,
            Long resolutionId,
            String resolutionNumber) {}

    public record DrawItem(
            Long id,
            LocalDate date,
            Long resolutionId,
            String resolutionNumber,
            List<DrawTeacherItem> teachers) {}

    public record DrawTeacherItem(Long teacherId, String teacherName) {}

    public record DefenseItem(Long id, LocalDate date, String result) {}
}

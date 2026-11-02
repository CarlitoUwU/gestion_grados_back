package com.claudecoders.grades.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@Tag(name = "Reportes", description = "Datos tabulares listos para exportación desde el frontend")
@SecurityRequirement(name = "bearer-jwt")
@Validated
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/statistics")
    @Operation(summary = "Reporte de estadísticas para acreditación")
    public ReportResponse statistics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long modalityId) {
        return service.statistics(year, schoolId, modalityId);
    }

    @GetMapping("/teachers")
    @Operation(summary = "Reporte de participaciones de un docente")
    public ReportResponse teachers(
            @RequestParam(required = false) @Positive Long teacherId,
            @RequestParam(required = false) @Min(1000) @Max(9999) Integer year) {
        return service.teachers(teacherId, year);
    }

    @GetMapping("/draws")
    @Operation(summary = "Reporte de sorteos externos")
    public ReportResponse draws(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long schoolId) {
        return service.draws(year, schoolId);
    }

    @GetMapping("/defended-works")
    @Operation(summary = "Reporte de trabajos sustentados")
    public ReportResponse defendedWorks(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long modalityId) {
        return service.defendedWorks(year, schoolId, modalityId);
    }
}

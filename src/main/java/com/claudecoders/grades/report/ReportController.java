package com.claudecoders.grades.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@Tag(name = "Reportes", description = "Datos tabulares listos para exportación desde el frontend")
@SecurityRequirement(name = "bearer-jwt")
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
    public ReportResponse teachers(@RequestParam(required = false) Long teacherId) {
        return service.teachers(teacherId);
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

package com.claudecoders.grades.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Indicadores para el inicio administrativo")
@SecurityRequirement(name = "bearer-jwt")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Obtener indicadores del panel de inicio")
    public DashboardResponse dashboard(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long schoolId) {
        return service.dashboard(year, schoolId);
    }
}

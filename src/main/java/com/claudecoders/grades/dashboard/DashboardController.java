package com.claudecoders.grades.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
            summary = "Obtener indicadores del panel de inicio",
            description =
                    "El corte y todos los cálculos usan la zona America/Lima. Los filtros se"
                            + " aplican a los conteos, la distribución y ambas listas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Indicadores calculados"),
        @ApiResponse(
                responseCode = "400",
                description = "Filtro fuera de rango",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(ref = "#/components/schemas/ApiError"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o vencido"),
        @ApiResponse(responseCode = "403", description = "Usuario no autorizado"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public DashboardResponse dashboard(
            @Parameter(description = "Año de inicio del expediente", example = "2026")
                    @RequestParam(required = false)
                    Integer year,
            @Parameter(description = "ID positivo de la escuela del graduando", example = "5")
                    @RequestParam(required = false)
                    Long schoolId) {
        return service.dashboard(year, schoolId);
    }
}

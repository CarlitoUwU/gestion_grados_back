package com.claudecoders.grades.expedient;

import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/expedients")
@Tag(name = "Expedients", description = "Expedient CRUD")
@SecurityRequirement(name = "bearer-jwt")
public class ExpedientQueryController {
    private final ExpedientQueryService service;
    private final ExpedientVersionService versions;

    public ExpedientQueryController(
            ExpedientQueryService service, ExpedientVersionService versions) {
        this.service = service;
        this.versions = versions;
    }

    @GetMapping("/search")
    @Operation(
            summary = "Buscar expedientes",
            description =
                    "Consulta segura con parámetros JPA para búsqueda por expediente,"
                            + " graduando, escuela, resolución o docente.")
    public List<ExpedientApiModels.Summary> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) Long modalityId,
            @Parameter(description = "Año de inicio del expediente") @RequestParam(required = false)
                    Integer year) {
        return service.search(search, schoolId, statusId, modalityId, year);
    }

    @GetMapping("/{id}/detail")
    @Operation(
            summary = "Obtener expediente con resoluciones, asesor, jurado, sorteos y sustentación",
            description =
                    "La cabecera ETag identifica la versión del agregado; enviarla como If-Match"
                            + " en PUT /expedients/{id}/academic evita sobrescribir cambios ajenos.")
    public org.springframework.http.ResponseEntity<ExpedientApiModels.Detail> detail(
            @PathVariable Long id) {
        var detail = service.detail(id);
        return org.springframework.http.ResponseEntity.ok()
                .eTag(versions.etag(detail))
                .body(detail);
    }
}

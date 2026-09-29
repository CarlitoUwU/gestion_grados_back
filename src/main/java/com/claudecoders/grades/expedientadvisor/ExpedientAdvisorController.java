package com.claudecoders.grades.expedientadvisor;

import com.claudecoders.grades.expedientadvisor.dto.ExpedientAdvisorRequest;
import com.claudecoders.grades.expedientadvisor.dto.ExpedientAdvisorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/expedient-advisors")
@Tag(name = "ExpedientAdvisors", description = "CRUD de asesores")
public class ExpedientAdvisorController {
    private final ExpedientAdvisorService service;

    public ExpedientAdvisorController(ExpedientAdvisorService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar asesores")
    @ApiResponse(responseCode = "200", description = "Encontrados")
    public ResponseEntity<List<ExpedientAdvisorResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener asesor")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Encontrado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<ExpedientAdvisorResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear asesor")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Creado"),
        @ApiResponse(responseCode = "400", description = "Inválido"),
        @ApiResponse(responseCode = "404", description = "Referencia no encontrada")
    })
    public ResponseEntity<ExpedientAdvisorResponse> create(
            @Valid @RequestBody ExpedientAdvisorRequest request) {
        var r = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/expedient-advisors/" + r.id())).body(r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar asesor")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Actualizado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<ExpedientAdvisorResponse> update(
            @PathVariable Long id, @Valid @RequestBody ExpedientAdvisorRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar asesor")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Eliminado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

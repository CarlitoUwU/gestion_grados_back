package com.claudecoders.grades.resolution;

import com.claudecoders.grades.resolution.dto.ResolutionRequest;
import com.claudecoders.grades.resolution.dto.ResolutionResponse;
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
@RequestMapping("/resolutions")
@Tag(name = "Resolutions", description = "CRUD de resoluciones")
public class ResolutionController {
    private final ResolutionService service;

    public ResolutionController(ResolutionService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar resoluciones")
    @ApiResponse(responseCode = "200", description = "Encontradas")
    public ResponseEntity<List<ResolutionResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener resolución")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Encontrada"),
        @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    public ResponseEntity<ResolutionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear resolución")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Creada"),
        @ApiResponse(responseCode = "400", description = "Inválida"),
        @ApiResponse(responseCode = "404", description = "Expediente no encontrado"),
        @ApiResponse(responseCode = "409", description = "Número duplicado")
    })
    public ResponseEntity<ResolutionResponse> create(
            @Valid @RequestBody ResolutionRequest request) {
        var r = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/resolutions/" + r.id())).body(r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar resolución")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Actualizada"),
        @ApiResponse(responseCode = "404", description = "No encontrada"),
        @ApiResponse(responseCode = "409", description = "Conflicto")
    })
    public ResponseEntity<ResolutionResponse> update(
            @PathVariable Long id, @Valid @RequestBody ResolutionRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar resolución")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Eliminada"),
        @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

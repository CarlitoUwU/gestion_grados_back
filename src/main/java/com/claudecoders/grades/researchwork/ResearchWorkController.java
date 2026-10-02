package com.claudecoders.grades.researchwork;

import com.claudecoders.grades.researchwork.dto.ResearchWorkRequest;
import com.claudecoders.grades.researchwork.dto.ResearchWorkResponse;
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
@RequestMapping("/research-works")
@Tag(name = "Research works", description = "Research work CRUD")
public class ResearchWorkController {
    private final ResearchWorkService service;

    public ResearchWorkController(ResearchWorkService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar trabajos")
    @ApiResponse(responseCode = "200", description = "Encontrados")
    public ResponseEntity<List<ResearchWorkResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener trabajo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Encontrado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<ResearchWorkResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear trabajo")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Creado"),
        @ApiResponse(responseCode = "400", description = "Inválido"),
        @ApiResponse(responseCode = "404", description = "Expediente no encontrado"),
        @ApiResponse(responseCode = "409", description = "Ya existe para el expediente")
    })
    public ResponseEntity<ResearchWorkResponse> create(
            @Valid @RequestBody ResearchWorkRequest request) {
        var r = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/research-works/" + r.id())).body(r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar trabajo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Actualizado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<ResearchWorkResponse> update(
            @PathVariable Long id, @Valid @RequestBody ResearchWorkRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar trabajo")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Eliminado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

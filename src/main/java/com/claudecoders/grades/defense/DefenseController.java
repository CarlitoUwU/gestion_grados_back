package com.claudecoders.grades.defense;

import com.claudecoders.grades.defense.dto.DefenseRequest;
import com.claudecoders.grades.defense.dto.DefenseResponse;
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
@RequestMapping("/defenses")
@Tag(name = "Defenses", description = "CRUD de defensas")
public class DefenseController {
    private final DefenseService service;

    public DefenseController(DefenseService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar defensas")
    @ApiResponse(responseCode = "200", description = "Encontradas")
    public ResponseEntity<List<DefenseResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener defensa")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Encontrada"),
        @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    public ResponseEntity<DefenseResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear defensa")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Creada"),
        @ApiResponse(responseCode = "400", description = "Inválida"),
        @ApiResponse(responseCode = "404", description = "Expediente no encontrado"),
        @ApiResponse(responseCode = "409", description = "Ya existe para el expediente")
    })
    public ResponseEntity<DefenseResponse> create(@Valid @RequestBody DefenseRequest request) {
        var r = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/defenses/" + r.id())).body(r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar defensa")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Actualizada"),
        @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    public ResponseEntity<DefenseResponse> update(
            @PathVariable Long id, @Valid @RequestBody DefenseRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar defensa")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Eliminada"),
        @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.claudecoders.grades.graduate;

import com.claudecoders.grades.graduate.dto.GraduateRequest;
import com.claudecoders.grades.graduate.dto.GraduateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/graduates")
@Tag(name = "Graduates", description = "CRUD de egresados")
public class GraduateController {
    private final GraduateService service;

    public GraduateController(GraduateService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar egresados")
    @ApiResponse(responseCode = "200", description = "Encontrados")
    public ResponseEntity<List<GraduateResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener egresado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Encontrado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<GraduateResponse> findById(
            @Parameter(description = "ID") @PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear egresado")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Creado"),
        @ApiResponse(responseCode = "400", description = "Inválido"),
        @ApiResponse(responseCode = "404", description = "Escuela no encontrada")
    })
    public ResponseEntity<GraduateResponse> create(@Valid @RequestBody GraduateRequest request) {
        var r = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/graduates/" + r.id())).body(r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar egresado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Actualizado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<GraduateResponse> update(
            @PathVariable Long id, @Valid @RequestBody GraduateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar egresado")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Eliminado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

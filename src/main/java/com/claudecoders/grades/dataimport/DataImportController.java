package com.claudecoders.grades.dataimport;

import com.claudecoders.grades.dataimport.dto.DataImportRequest;
import com.claudecoders.grades.dataimport.dto.DataImportResponse;
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
@RequestMapping("/data-imports")
@Tag(name = "Data imports", description = "Data import CRUD")
public class DataImportController {
    private final DataImportService service;

    public DataImportController(DataImportService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar importaciones")
    @ApiResponse(responseCode = "200", description = "Encontradas")
    public ResponseEntity<List<DataImportResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener importación")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Encontrada"),
        @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    public ResponseEntity<DataImportResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear importación")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Creada"),
        @ApiResponse(responseCode = "400", description = "Inválida"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<DataImportResponse> create(
            @Valid @RequestBody DataImportRequest request) {
        var r = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/data-imports/" + r.id())).body(r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar importación")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Actualizada"),
        @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    public ResponseEntity<DataImportResponse> update(
            @PathVariable Long id, @Valid @RequestBody DataImportRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar importación")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Eliminada"),
        @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.claudecoders.grades.expedient;

import com.claudecoders.grades.expedient.dto.ExpedientRequest;
import com.claudecoders.grades.expedient.dto.ExpedientResponse;
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
@RequestMapping("/expedients")
@Tag(name = "Expedients", description = "Expedient CRUD")
public class ExpedientController {
    private final ExpedientService service;

    public ExpedientController(ExpedientService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar expedientes")
    @ApiResponse(responseCode = "200", description = "Encontrados")
    public ResponseEntity<List<ExpedientResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener expediente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Encontrado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<ExpedientResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear expediente")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Creado"),
        @ApiResponse(responseCode = "400", description = "Inválido"),
        @ApiResponse(responseCode = "404", description = "Referencia no encontrada"),
        @ApiResponse(responseCode = "409", description = "Número duplicado")
    })
    public ResponseEntity<ExpedientResponse> create(@Valid @RequestBody ExpedientRequest request) {
        var r = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/expedients/" + r.id())).body(r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar expediente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Actualizado"),
        @ApiResponse(responseCode = "404", description = "No encontrado"),
        @ApiResponse(responseCode = "409", description = "Número duplicado")
    })
    public ResponseEntity<ExpedientResponse> update(
            @PathVariable Long id, @Valid @RequestBody ExpedientRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar expediente")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Eliminado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

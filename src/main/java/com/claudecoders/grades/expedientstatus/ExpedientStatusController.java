package com.claudecoders.grades.expedientstatus;

import com.claudecoders.grades.expedientstatus.dto.ExpedientStatusRequest;
import com.claudecoders.grades.expedientstatus.dto.ExpedientStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/expedient-statuses")
@Tag(name = "Expedient statuses", description = "CRUD de estados de expediente")
public class ExpedientStatusController {
    private final ExpedientStatusService service;

    public ExpedientStatusController(ExpedientStatusService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar estados")
    @ApiResponse(responseCode = "200", description = "Estados encontrados")
    public ResponseEntity<List<ExpedientStatusResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un estado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado encontrado"),
        @ApiResponse(responseCode = "404", description = "Estado no encontrado")
    })
    public ResponseEntity<ExpedientStatusResponse> findById(
            @Parameter(description = "ID del estado") @PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear un estado")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Estado creado"),
        @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    })
    public ResponseEntity<ExpedientStatusResponse> create(
            @Valid @RequestBody ExpedientStatusRequest request) {
        ExpedientStatusResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/expedient-statuses/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un estado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado actualizado"),
        @ApiResponse(responseCode = "404", description = "Estado no encontrado")
    })
    public ResponseEntity<ExpedientStatusResponse> update(
            @Parameter(description = "ID del estado") @PathVariable Long id,
            @Valid @RequestBody ExpedientStatusRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un estado")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Estado eliminado"),
        @ApiResponse(responseCode = "404", description = "Estado no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID del estado") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

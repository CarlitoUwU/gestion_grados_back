package com.claudecoders.grades.school;

import com.claudecoders.grades.school.dto.SchoolRequest;
import com.claudecoders.grades.school.dto.SchoolResponse;
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
@RequestMapping("/schools")
@Tag(name = "Schools", description = "School CRUD")
public class SchoolController {

    private final SchoolService service;

    public SchoolController(SchoolService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar escuelas")
    @ApiResponse(responseCode = "200", description = "Escuelas encontradas")
    public ResponseEntity<List<SchoolResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una escuela")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Escuela encontrada"),
        @ApiResponse(responseCode = "404", description = "Escuela no encontrada")
    })
    public ResponseEntity<SchoolResponse> findById(
            @Parameter(description = "ID de la escuela") @PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear una escuela")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Escuela creada"),
        @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    })
    public ResponseEntity<SchoolResponse> create(@Valid @RequestBody SchoolRequest request) {
        SchoolResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/schools/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una escuela")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Escuela actualizada"),
        @ApiResponse(responseCode = "404", description = "Escuela no encontrada")
    })
    public ResponseEntity<SchoolResponse> update(
            @Parameter(description = "ID de la escuela") @PathVariable Long id,
            @Valid @RequestBody SchoolRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una escuela")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Escuela eliminada"),
        @ApiResponse(responseCode = "404", description = "Escuela no encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de la escuela") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

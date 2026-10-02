package com.claudecoders.grades.modality;

import com.claudecoders.grades.modality.dto.DegreeModalityRequest;
import com.claudecoders.grades.modality.dto.DegreeModalityResponse;
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
@RequestMapping("/degree-modalities")
@Tag(name = "Degree modalities", description = "Degree modality CRUD")
public class DegreeModalityController {
    private final DegreeModalityService service;

    public DegreeModalityController(DegreeModalityService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar modalidades")
    @ApiResponse(responseCode = "200", description = "Modalidades encontradas")
    public ResponseEntity<List<DegreeModalityResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una modalidad")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Modalidad encontrada"),
        @ApiResponse(responseCode = "404", description = "Modalidad no encontrada")
    })
    public ResponseEntity<DegreeModalityResponse> findById(
            @Parameter(description = "ID de la modalidad") @PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear una modalidad")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Modalidad creada"),
        @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    })
    public ResponseEntity<DegreeModalityResponse> create(
            @Valid @RequestBody DegreeModalityRequest request) {
        DegreeModalityResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/degree-modalities/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una modalidad")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Modalidad actualizada"),
        @ApiResponse(responseCode = "404", description = "Modalidad no encontrada")
    })
    public ResponseEntity<DegreeModalityResponse> update(
            @Parameter(description = "ID de la modalidad") @PathVariable Long id,
            @Valid @RequestBody DegreeModalityRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una modalidad")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Modalidad eliminada"),
        @ApiResponse(responseCode = "404", description = "Modalidad no encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de la modalidad") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

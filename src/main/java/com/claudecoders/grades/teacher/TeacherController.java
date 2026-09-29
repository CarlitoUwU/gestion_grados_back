package com.claudecoders.grades.teacher;

import com.claudecoders.grades.teacher.dto.TeacherRequest;
import com.claudecoders.grades.teacher.dto.TeacherResponse;
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
@RequestMapping("/teachers")
@Tag(name = "Teachers", description = "CRUD de docentes")
public class TeacherController {

    private final TeacherService service;

    public TeacherController(TeacherService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar docentes")
    @ApiResponse(responseCode = "200", description = "Docentes encontrados")
    public ResponseEntity<List<TeacherResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un docente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Docente encontrado"),
        @ApiResponse(responseCode = "404", description = "Docente no encontrado")
    })
    public ResponseEntity<TeacherResponse> findById(
            @Parameter(description = "ID del docente") @PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear un docente")
    @ApiResponse(responseCode = "201", description = "Docente creado")
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody TeacherRequest request) {
        TeacherResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/teachers/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un docente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Docente actualizado"),
        @ApiResponse(responseCode = "404", description = "Docente no encontrado")
    })
    public ResponseEntity<TeacherResponse> update(
            @Parameter(description = "ID del docente") @PathVariable Long id,
            @Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un docente")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Docente eliminado"),
        @ApiResponse(responseCode = "404", description = "Docente no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID del docente") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

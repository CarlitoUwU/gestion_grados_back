package com.claudecoders.grades.jurydraw;

import com.claudecoders.grades.jurydraw.dto.JuryDrawRequest;
import com.claudecoders.grades.jurydraw.dto.JuryDrawResponse;
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
@RequestMapping("/jury-draws")
@Tag(name = "JuryDraws", description = "CRUD de sorteos")
public class JuryDrawController {
    private final JuryDrawService service;

    public JuryDrawController(JuryDrawService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar sorteos")
    @ApiResponse(responseCode = "200", description = "Encontrados")
    public ResponseEntity<List<JuryDrawResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener sorteo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Encontrado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<JuryDrawResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear sorteo")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Creado"),
        @ApiResponse(responseCode = "400", description = "Inválido"),
        @ApiResponse(responseCode = "404", description = "Referencia no encontrada")
    })
    public ResponseEntity<JuryDrawResponse> create(@Valid @RequestBody JuryDrawRequest request) {
        var r = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/jury-draws/" + r.id())).body(r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar sorteo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Actualizado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<JuryDrawResponse> update(
            @PathVariable Long id, @Valid @RequestBody JuryDrawRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar sorteo")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Eliminado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.claudecoders.grades.jurymember;

import com.claudecoders.grades.jurymember.dto.JuryMemberRequest;
import com.claudecoders.grades.jurymember.dto.JuryMemberResponse;
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
@RequestMapping("/jury-members")
@Tag(name = "JuryMembers", description = "CRUD de miembros de jurado")
public class JuryMemberController {
    private final JuryMemberService service;

    public JuryMemberController(JuryMemberService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar miembros")
    @ApiResponse(responseCode = "200", description = "Encontrados")
    public ResponseEntity<List<JuryMemberResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener miembro")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Encontrado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<JuryMemberResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear miembro")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Creado"),
        @ApiResponse(responseCode = "400", description = "Inválido"),
        @ApiResponse(responseCode = "404", description = "Referencia no encontrada"),
        @ApiResponse(responseCode = "409", description = "Duplicado")
    })
    public ResponseEntity<JuryMemberResponse> create(
            @Valid @RequestBody JuryMemberRequest request) {
        var r = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/jury-members/" + r.id())).body(r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar miembro")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Actualizado"),
        @ApiResponse(responseCode = "404", description = "No encontrado"),
        @ApiResponse(responseCode = "409", description = "Duplicado")
    })
    public ResponseEntity<JuryMemberResponse> update(
            @PathVariable Long id, @Valid @RequestBody JuryMemberRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar miembro")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Eliminado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

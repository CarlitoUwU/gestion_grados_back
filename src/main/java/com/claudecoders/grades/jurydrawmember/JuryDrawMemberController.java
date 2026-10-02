package com.claudecoders.grades.jurydrawmember;

import com.claudecoders.grades.jurydrawmember.dto.JuryDrawMemberRequest;
import com.claudecoders.grades.jurydrawmember.dto.JuryDrawMemberResponse;
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
@RequestMapping("/jury-draw-members")
@Tag(name = "Jury draw members", description = "Jury draw member CRUD")
public class JuryDrawMemberController {
    private final JuryDrawMemberService service;

    public JuryDrawMemberController(JuryDrawMemberService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar miembros de sorteos")
    @ApiResponse(responseCode = "200", description = "Encontrados")
    public ResponseEntity<List<JuryDrawMemberResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener miembro de sorteo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Encontrado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<JuryDrawMemberResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear miembro de sorteo")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Creado"),
        @ApiResponse(responseCode = "400", description = "Inválido"),
        @ApiResponse(responseCode = "404", description = "Referencia no encontrada"),
        @ApiResponse(responseCode = "409", description = "Duplicado")
    })
    public ResponseEntity<JuryDrawMemberResponse> create(
            @Valid @RequestBody JuryDrawMemberRequest request) {
        var r = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/jury-draw-members/" + r.id())).body(r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar miembro de sorteo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Actualizado"),
        @ApiResponse(responseCode = "404", description = "No encontrado"),
        @ApiResponse(responseCode = "409", description = "Duplicado")
    })
    public ResponseEntity<JuryDrawMemberResponse> update(
            @PathVariable Long id, @Valid @RequestBody JuryDrawMemberRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar miembro de sorteo")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Eliminado"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

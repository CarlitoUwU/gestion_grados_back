package com.claudecoders.grades.expedient;

import com.claudecoders.grades.expedient.dto.AcademicRequest;
import com.claudecoders.grades.expedient.dto.CompleteExpedientRequest;
import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import com.claudecoders.grades.expedient.dto.ExpedientRequest;
import com.claudecoders.grades.expedient.dto.ExpedientResponse;
import com.claudecoders.grades.expedient.dto.StatusUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/expedients")
@Tag(name = "Expedients", description = "Expedient CRUD")
@SecurityRequirement(name = "bearer-jwt")
public class ExpedientController {
    private final ExpedientService service;
    private final ExpedientAcademicService academic;

    public ExpedientController(ExpedientService service, ExpedientAcademicService academic) {
        this.service = service;
        this.academic = academic;
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
    public ResponseEntity<ExpedientResponse> create(
            @Valid @RequestBody ExpedientRequest request, Authentication authentication) {
        var r = service.create(request, authentication);
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
            @PathVariable Long id,
            @Valid @RequestBody ExpedientRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.update(id, request, authentication));
    }

    @PostMapping("/complete")
    @Operation(summary = "Registrar expediente, graduando e investigación en una transacción")
    public ResponseEntity<ExpedientApiModels.Detail> createComplete(
            @Valid @RequestBody CompleteExpedientRequest request, Authentication authentication) {
        var detail = service.createComplete(request, authentication);
        return ResponseEntity.created(
                        URI.create("/api/v1/expedients/" + detail.summary().id() + "/detail"))
                .body(detail);
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Actualizar expediente, graduando e investigación")
    public ExpedientApiModels.Detail updateComplete(
            @PathVariable Long id,
            @Valid @RequestBody CompleteExpedientRequest request,
            Authentication authentication) {
        return service.updateComplete(id, request, authentication);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar estado del expediente sin restricciones de transición")
    public ExpedientApiModels.Summary updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication authentication) {
        return service.updateStatus(id, request.statusId(), authentication);
    }

    @PutMapping("/{id}/academic")
    @Operation(
            summary = "Reemplazar toda la información académica",
            description =
                    "Reemplazo total y atómico: omitir elementos de las listas los elimina; "
                            + "advisor y defense nulos los eliminan. clientId identifica una resolución "
                            + "en esta petición; resolutionClientId es opcional. fileUrl no se modifica.")
    public ExpedientApiModels.Detail replaceAcademic(
            @PathVariable Long id,
            @Valid @RequestBody AcademicRequest request,
            Authentication authentication) {
        return academic.replace(id, request, authentication);
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

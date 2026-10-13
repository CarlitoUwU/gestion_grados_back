package com.claudecoders.grades.expedient;

import com.claudecoders.grades.expedient.dto.AcademicRequest;
import com.claudecoders.grades.expedient.dto.CompleteExpedientRequest;
import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import com.claudecoders.grades.expedient.dto.ExpedientProcessRequests;
import com.claudecoders.grades.expedient.dto.ExpedientRequest;
import com.claudecoders.grades.expedient.dto.ExpedientResponse;
import com.claudecoders.grades.expedient.dto.ExpedientWriteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/expedients")
@Tag(name = "Expedients", description = "Gestión unificada de expedientes y proceso académico")
@SecurityRequirement(name = "bearer-jwt")
public class ExpedientController {
    private final ExpedientService service;
    private final ExpedientQueryService queries;
    private final ExpedientProcessService processes;
    private final ExpedientAcademicService academic;

    public ExpedientController(
            ExpedientService service,
            ExpedientQueryService queries,
            ExpedientProcessService processes,
            ExpedientAcademicService academic) {
        this.service = service;
        this.queries = queries;
        this.processes = processes;
        this.academic = academic;
    }

    @GetMapping
    @Operation(summary = "Listar expedientes con filtros y paginación")
    public ExpedientApiModels.PageResult findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) Long modalityId,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return queries.search(search, schoolId, statusId, modalityId, year, page, size);
    }

    @GetMapping("/{number}")
    @Operation(summary = "Obtener detalle completo por código de expediente")
    public ExpedientApiModels.Detail findByNumber(@PathVariable String number) {
        return queries.detail(number);
    }

    @PostMapping
    @Operation(summary = "Crear expediente, graduando y trabajo en una transacción")
    public ResponseEntity<ExpedientApiModels.Detail> create(
            @Valid @RequestBody ExpedientWriteRequest request, Authentication auth) {
        var result = service.create(request, auth);
        var location =
                UriComponentsBuilder.fromPath("/api/v1/expedients/{number}")
                        .buildAndExpand(result.summary().number())
                        .encode()
                        .toUri();
        return ResponseEntity.created(location).body(result);
    }

    @PutMapping("/{number}")
    @Operation(summary = "Actualizar expediente, graduando y trabajo")
    public ExpedientApiModels.Detail update(
            @PathVariable String number,
            @Valid @RequestBody ExpedientWriteRequest request,
            Authentication auth) {
        return service.update(number, request, auth);
    }

    @PostMapping("/complete")
    @Operation(summary = "Registrar expediente, graduando e investigación en una transacción")
    public ResponseEntity<ExpedientApiModels.Detail> createComplete(
            @Valid @RequestBody CompleteExpedientRequest request, Authentication authentication) {
        var detail = service.createComplete(request, authentication);
        return ResponseEntity.created(
                        java.net.URI.create(
                                "/api/v1/expedients/" + detail.summary().id() + "/detail"))
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

    @PostMapping("/legacy")
    @Operation(summary = "Crear expediente con referencias por ID (contrato heredado)")
    public ResponseEntity<ExpedientResponse> createLegacy(
            @Valid @RequestBody ExpedientRequest request, Authentication auth) {
        var result = service.create(request, auth);
        return ResponseEntity.created(
                        java.net.URI.create("/api/v1/expedients/" + result.id() + "/detail"))
                .body(result);
    }

    @DeleteMapping("/{number}")
    @Operation(summary = "Eliminar expediente y sus registros académicos asociados")
    public ResponseEntity<Void> delete(@PathVariable String number) {
        service.delete(number);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{number}/status")
    @Operation(summary = "Cambiar estado y observaciones administrativas")
    public Object status(
            @PathVariable String number,
            @Valid @RequestBody ExpedientProcessRequests.Status request,
            Authentication auth) {
        if (request.statusId() != null) {
            try {
                return service.updateStatus(Long.valueOf(number), request.statusId(), auth);
            } catch (NumberFormatException exception) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "statusId requiere el ID num?rico del expediente");
            }
        }
        return processes.status(number, request, auth);
    }

    @PutMapping("/{number}/advisor")
    @Operation(summary = "Asignar o editar asesor y resolución de decanato")
    public ExpedientApiModels.Detail advisor(
            @PathVariable String number,
            @Valid @RequestBody ExpedientProcessRequests.Advisor request,
            Authentication auth) {
        return processes.advisor(number, request, auth);
    }

    @PutMapping("/{number}/jury")
    @Operation(summary = "Asignar o editar Presidente, Secretario y Vocal")
    public ExpedientApiModels.Detail jury(
            @PathVariable String number,
            @Valid @RequestBody ExpedientProcessRequests.Jury request,
            Authentication auth) {
        return processes.jury(number, request, auth);
    }

    @PostMapping("/{number}/draw")
    @Operation(summary = "Registrar resultado del sorteo de jurado")
    public ResponseEntity<ExpedientApiModels.Detail> draw(
            @PathVariable String number,
            @Valid @RequestBody ExpedientProcessRequests.Draw request,
            Authentication auth) {
        return ResponseEntity.status(201).body(processes.draw(number, request, auth));
    }

    @PutMapping("/{number}/defense")
    @Operation(summary = "Registrar o actualizar sustentación y dictamen")
    public ExpedientApiModels.Detail defense(
            @PathVariable String number,
            @Valid @RequestBody ExpedientProcessRequests.Defense request,
            Authentication auth) {
        return processes.defense(number, request, auth);
    }
}

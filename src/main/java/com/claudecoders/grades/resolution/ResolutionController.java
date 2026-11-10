package com.claudecoders.grades.resolution;

import com.claudecoders.grades.resolution.dto.ResolutionRequest;
import com.claudecoders.grades.resolution.dto.ResolutionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/resolutions")
@Tag(name = "Resolutions", description = "Resolution CRUD")
@SecurityRequirement(name = "bearer-jwt")
public class ResolutionController {
    private final ResolutionService service;

    public ResolutionController(ResolutionService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar resoluciones")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Encontradas",
                content =
                        @Content(
                                mediaType = "application/json",
                                array =
                                        @ArraySchema(
                                                schema =
                                                        @Schema(
                                                                implementation =
                                                                        ResolutionResponse
                                                                                .class)))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o vencido"),
        @ApiResponse(responseCode = "403", description = "Usuario no autorizado"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<List<ResolutionResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener resolución")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Encontrada"),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o vencido"),
        @ApiResponse(responseCode = "403", description = "Usuario no autorizado"),
        @ApiResponse(responseCode = "404", description = "No encontrada"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResolutionResponse> findById(
            @Parameter(description = "ID de la resolución", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(
            summary = "Crear resolución",
            description =
                    "Actualiza en la misma transacción los metadatos de última modificación del"
                            + " expediente usando el usuario del JWT.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Creada",
                headers = @Header(name = "Location", description = "URI de la resolución creada")),
        @ApiResponse(
                responseCode = "400",
                description = "Datos inválidos",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(ref = "#/components/schemas/ApiError"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o vencido"),
        @ApiResponse(responseCode = "403", description = "Usuario no autorizado"),
        @ApiResponse(responseCode = "404", description = "Expediente no encontrado"),
        @ApiResponse(responseCode = "409", description = "Número duplicado"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResolutionResponse> create(
            @Valid @RequestBody ResolutionRequest request, Authentication authentication) {
        var r = service.create(request, authentication);
        return ResponseEntity.created(URI.create("/api/v1/resolutions/" + r.id())).body(r);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar resolución",
            description =
                    "Los valores null de los campos opcionales se conservan como null. No se"
                            + " permite mover una resolución referenciada a otro expediente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Actualizada"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos inválidos",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(ref = "#/components/schemas/ApiError"))),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o vencido"),
        @ApiResponse(responseCode = "403", description = "Usuario no autorizado"),
        @ApiResponse(responseCode = "404", description = "Resolución o expediente no encontrado"),
        @ApiResponse(
                responseCode = "409",
                description = "Número duplicado o resolución referenciada"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<ResolutionResponse> update(
            @Parameter(description = "ID de la resolución", example = "1") @PathVariable Long id,
            @Valid @RequestBody ResolutionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.update(id, request, authentication));
    }

    @PostMapping(value = "/{id}/document", consumes = "multipart/form-data")
    @Operation(
            summary = "Subir PDF de resolución",
            description =
                    "PDF de hasta 10 MiB; se almacena en PostgreSQL y su descarga requiere JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Documento guardado"),
        @ApiResponse(responseCode = "400", description = "Archivo ausente o PDF inválido"),
        @ApiResponse(responseCode = "404", description = "Resolución inexistente"),
        @ApiResponse(responseCode = "413", description = "Supera 10 MiB")
    })
    public ResolutionResponse uploadDocument(
            @PathVariable Long id,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Authentication authentication)
            throws IOException {
        String url =
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/resolutions/{id}/document")
                        .buildAndExpand(id)
                        .toUriString();
        return service.uploadDocument(id, file, url, authentication);
    }

    @GetMapping(value = "/{id}/document", produces = "application/pdf")
    @Operation(
            summary = "Descargar PDF de resolución",
            description = "Requiere la misma autorización que el CRUD de resoluciones.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF"),
        @ApiResponse(responseCode = "404", description = "Documento inexistente")
    })
    public ResponseEntity<byte[]> document(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header("Content-Disposition", "attachment; filename=\"resolucion-" + id + ".pdf\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(service.document(id));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar resolución",
            description =
                    "Devuelve 409 si la resolución está referenciada por asesor, jurado o sorteo.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Eliminada sin body"),
        @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o vencido"),
        @ApiResponse(responseCode = "403", description = "Usuario no autorizado"),
        @ApiResponse(responseCode = "404", description = "No encontrada"),
        @ApiResponse(responseCode = "409", description = "Resolución en uso"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de la resolución", example = "1") @PathVariable Long id,
            Authentication authentication) {
        service.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}

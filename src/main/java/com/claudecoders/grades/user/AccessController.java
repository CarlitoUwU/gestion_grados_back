package com.claudecoders.grades.user;

import com.claudecoders.grades.user.dto.UserRequest;
import com.claudecoders.grades.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accesses")
@Tag(name = "Accesses", description = "Registered users with system access")
@SecurityRequirement(name = "bearer-jwt")
public class AccessController {
    private final UserService users;
    private final com.claudecoders.grades.auth.CurrentUserService currentUser;

    public AccessController(
            UserService users, com.claudecoders.grades.auth.CurrentUserService currentUser) {
        this.currentUser = currentUser;
        this.users = users;
    }

    @GetMapping
    @Operation(summary = "Listar accesos administrativos")
    public List<UserResponse> findAll() {
        return users.findAll();
    }

    @ApiResponse(responseCode = "201", description = "Creado")
    @PostMapping
    @Operation(
            summary = "Registrar acceso administrativo",
            description =
                    "Crea un usuario del sistema. El rol operativo esperado es ADMIN_GT"
                            + " porque no se manejarán roles diferenciados.")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse response = users.create(request);
        return ResponseEntity.created(URI.create("/api/v1/accesses/" + response.id()))
                .body(response);
    }

    @PatchMapping("/{id}/active")
    @Operation(
            summary = "Activar o desactivar acceso",
            description = "Un administrador no puede desactivar su propia cuenta (409).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Intento de desactivar la cuenta de la sesión actual")
    public ResponseEntity<UserResponse> active(
            @PathVariable Long id,
            @Valid @RequestBody AccessStatusRequest request,
            org.springframework.security.core.Authentication authentication) {
        if (!request.active() && id.equals(currentUser.from(authentication).getId()))
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "No puedes desactivar tu propia cuenta. Pide a otro administrador que lo haga.");
        return ResponseEntity.ok(users.setActive(id, request.active()));
    }

    public record AccessStatusRequest(@jakarta.validation.constraints.NotNull Boolean active) {}
}

package com.claudecoders.grades.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Sesión autenticada del usuario registrado en el sistema")
public class AuthController {
    private final CurrentUserService currentUser;

    public AuthController(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    @Operation(
            summary = "Obtener usuario autenticado",
            description =
                    "Devuelve el usuario activo registrado en users asociado al JWT de Google."
                            + " El sistema usa un único rol operativo: ADMIN_GT.",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(CurrentUserResponse.from(currentUser.from(authentication)));
    }
}

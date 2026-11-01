package com.claudecoders.grades.auth;

import com.claudecoders.grades.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Usuario autenticado registrado en el sistema")
public record CurrentUserResponse(
        @Schema(minimum = "1") Long id,
        String email,
        String fullName,
        @Schema(nullable = true) String googleSubject,
        String role,
        boolean active) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getGoogleSubject(),
                user.getRole(),
                user.isActive());
    }
}

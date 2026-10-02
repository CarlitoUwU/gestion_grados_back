package com.claudecoders.grades.auth;

import com.claudecoders.grades.user.User;

public record CurrentUserResponse(
        Long id, String email, String fullName, String googleSubject, String role, boolean active) {
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

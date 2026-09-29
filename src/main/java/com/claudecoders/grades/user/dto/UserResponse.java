package com.claudecoders.grades.user.dto;

import com.claudecoders.grades.user.User;

public record UserResponse(
        Long id, String email, String fullName, String googleSubject, String role, boolean active) {
    public static UserResponse from(User value) {
        return new UserResponse(
                value.getId(),
                value.getEmail(),
                value.getFullName(),
                value.getGoogleSubject(),
                value.getRole(),
                value.isActive());
    }
}

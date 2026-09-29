package com.claudecoders.grades.expedientstatus.dto;

import com.claudecoders.grades.expedientstatus.ExpedientStatus;

public record ExpedientStatusResponse(Long id, String name, boolean active) {

    public static ExpedientStatusResponse from(ExpedientStatus status) {
        return new ExpedientStatusResponse(status.getId(), status.getName(), status.isActive());
    }
}

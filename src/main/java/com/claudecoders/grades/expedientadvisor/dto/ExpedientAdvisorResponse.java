package com.claudecoders.grades.expedientadvisor.dto;

import com.claudecoders.grades.expedientadvisor.ExpedientAdvisor;
import java.time.LocalDate;

public record ExpedientAdvisorResponse(
        Long id, Long expedientId, Long teacherId, Long resolutionId, LocalDate assignedAt) {
    public static ExpedientAdvisorResponse from(ExpedientAdvisor value) {
        return new ExpedientAdvisorResponse(
                value.getId(),
                value.getExpedient().getId(),
                value.getTeacher().getId(),
                value.getResolution() == null ? null : value.getResolution().getId(),
                value.getAssignedAt());
    }
}

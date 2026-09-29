package com.claudecoders.grades.jurydraw.dto;

import com.claudecoders.grades.jurydraw.JuryDraw;
import java.time.LocalDate;

public record JuryDrawResponse(Long id, Long expedientId, Long resolutionId, LocalDate drawDate) {
    public static JuryDrawResponse from(JuryDraw value) {
        return new JuryDrawResponse(
                value.getId(),
                value.getExpedient().getId(),
                value.getResolution() == null ? null : value.getResolution().getId(),
                value.getDrawDate());
    }
}

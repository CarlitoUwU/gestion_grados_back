package com.claudecoders.grades.resolution.dto;

import com.claudecoders.grades.resolution.Resolution;
import java.time.LocalDate;

public record ResolutionResponse(
        Long id,
        Long expedientId,
        String number,
        LocalDate resolutionDate,
        String resolutionType,
        String fileUrl) {
    public static ResolutionResponse from(Resolution value) {
        return new ResolutionResponse(
                value.getId(),
                value.getExpedient().getId(),
                value.getNumber(),
                value.getResolutionDate(),
                value.getResolutionType(),
                value.getFileUrl());
    }
}

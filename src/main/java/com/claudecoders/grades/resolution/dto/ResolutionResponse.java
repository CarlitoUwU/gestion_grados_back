package com.claudecoders.grades.resolution.dto;

import com.claudecoders.grades.resolution.Resolution;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Resolución persistida")
public record ResolutionResponse(
        @Schema(minimum = "1") Long id,
        @Schema(minimum = "1") Long expedientId,
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

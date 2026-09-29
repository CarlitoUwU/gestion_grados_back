package com.claudecoders.grades.expedient.dto;

import com.claudecoders.grades.expedient.Expedient;

public record ExpedientResponse(
        Long id,
        String number,
        java.time.LocalDate startDate,
        Long graduateId,
        Long modalityId,
        Long statusId,
        Long createdById,
        Long updatedById) {
    public static ExpedientResponse from(Expedient value) {
        return new ExpedientResponse(
                value.getId(),
                value.getNumber(),
                value.getStartDate(),
                value.getGraduate().getId(),
                value.getModality() == null ? null : value.getModality().getId(),
                value.getStatus() == null ? null : value.getStatus().getId(),
                value.getCreatedBy() == null ? null : value.getCreatedBy().getId(),
                value.getUpdatedBy() == null ? null : value.getUpdatedBy().getId());
    }
}

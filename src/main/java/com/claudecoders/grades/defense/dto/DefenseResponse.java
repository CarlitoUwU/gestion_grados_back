package com.claudecoders.grades.defense.dto;

import com.claudecoders.grades.defense.Defense;
import java.time.LocalDate;

public record DefenseResponse(Long id, Long expedientId, LocalDate defenseDate, String result) {
    public static DefenseResponse from(Defense value) {
        return new DefenseResponse(
                value.getId(),
                value.getExpedient().getId(),
                value.getDefenseDate(),
                value.getResult());
    }
}

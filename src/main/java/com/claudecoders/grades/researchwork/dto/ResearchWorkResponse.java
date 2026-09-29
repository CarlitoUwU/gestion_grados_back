package com.claudecoders.grades.researchwork.dto;

import com.claudecoders.grades.researchwork.ResearchWork;

public record ResearchWorkResponse(Long id, Long expedientId, String workType, String title) {
    public static ResearchWorkResponse from(ResearchWork value) {
        return new ResearchWorkResponse(
                value.getId(), value.getExpedient().getId(), value.getWorkType(), value.getTitle());
    }
}

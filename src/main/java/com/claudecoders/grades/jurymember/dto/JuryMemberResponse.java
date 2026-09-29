package com.claudecoders.grades.jurymember.dto;

import com.claudecoders.grades.jurymember.JuryMember;
import com.claudecoders.grades.jurymember.JuryRole;

public record JuryMemberResponse(
        Long id, Long expedientId, Long teacherId, Long resolutionId, JuryRole juryRole) {
    public static JuryMemberResponse from(JuryMember value) {
        return new JuryMemberResponse(
                value.getId(),
                value.getExpedient().getId(),
                value.getTeacher().getId(),
                value.getResolution() == null ? null : value.getResolution().getId(),
                value.getJuryRole());
    }
}

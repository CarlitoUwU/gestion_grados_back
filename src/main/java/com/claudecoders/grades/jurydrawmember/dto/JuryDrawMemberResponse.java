package com.claudecoders.grades.jurydrawmember.dto;

import com.claudecoders.grades.jurydrawmember.JuryDrawMember;

public record JuryDrawMemberResponse(Long id, Long juryDrawId, Long teacherId) {
    public static JuryDrawMemberResponse from(JuryDrawMember value) {
        return new JuryDrawMemberResponse(
                value.getId(), value.getJuryDraw().getId(), value.getTeacher().getId());
    }
}

package com.claudecoders.grades.jurymember.dto;

import com.claudecoders.grades.jurymember.JuryRole;
import jakarta.validation.constraints.NotNull;

public record JuryMemberRequest(
        @NotNull Long expedientId,
        @NotNull Long teacherId,
        Long resolutionId,
        @NotNull JuryRole juryRole) {}

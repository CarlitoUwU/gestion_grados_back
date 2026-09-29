package com.claudecoders.grades.jurydrawmember.dto;

import jakarta.validation.constraints.NotNull;

public record JuryDrawMemberRequest(@NotNull Long juryDrawId, @NotNull Long teacherId) {}

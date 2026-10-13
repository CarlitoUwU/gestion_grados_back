package com.claudecoders.grades.expedient.dto;

import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull Long statusId) {}

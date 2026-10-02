package com.claudecoders.grades.teacher.dto;

import java.time.LocalDate;

public record TeacherHistoryResponse(
        Long expedientId,
        String expedientNumber,
        String graduateName,
        String researchTitle,
        String schoolName,
        String participation,
        Long resolutionId,
        String resolutionNumber,
        LocalDate resolutionDate,
        String statusName) {}

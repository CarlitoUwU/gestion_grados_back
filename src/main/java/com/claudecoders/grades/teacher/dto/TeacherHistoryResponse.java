package com.claudecoders.grades.teacher.dto;

import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
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

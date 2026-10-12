package com.claudecoders.grades.dataimport.dto;

import java.time.LocalDate;

public record StagedGraduateRecord(
        int rowNumber,
        String expedientNumber,
        LocalDate startDate,
        String documentNumber,
        String firstNames,
        String lastNames,
        String schoolName,
        String academicProgram,
        String modalityName,
        String statusName,
        String researchTitle) {}

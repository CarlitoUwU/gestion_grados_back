package com.claudecoders.grades.graduate.dto;

import com.claudecoders.grades.graduate.Graduate;

public record GraduateResponse(
        Long id,
        String documentNumber,
        String firstNames,
        String lastNames,
        Long schoolId,
        String academicProgram) {
    public static GraduateResponse from(Graduate value) {
        return new GraduateResponse(
                value.getId(),
                value.getDocumentNumber(),
                value.getFirstNames(),
                value.getLastNames(),
                value.getSchool() == null ? null : value.getSchool().getId(),
                value.getAcademicProgram());
    }
}

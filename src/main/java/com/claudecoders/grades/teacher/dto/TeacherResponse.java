package com.claudecoders.grades.teacher.dto;

import com.claudecoders.grades.teacher.Teacher;

public record TeacherResponse(Long id, String documentNumber, String fullName, String email) {

    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
                teacher.getId(),
                teacher.getDocumentNumber(),
                teacher.getFullName(),
                teacher.getEmail());
    }
}

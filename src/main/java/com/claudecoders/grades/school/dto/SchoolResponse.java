package com.claudecoders.grades.school.dto;

import com.claudecoders.grades.school.School;

public record SchoolResponse(Long id, String name) {

    public static SchoolResponse from(School school) {
        return new SchoolResponse(school.getId(), school.getName());
    }
}

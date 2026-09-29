package com.claudecoders.grades.modality.dto;

import com.claudecoders.grades.modality.DegreeModality;

public record DegreeModalityResponse(Long id, String name, boolean active) {

    public static DegreeModalityResponse from(DegreeModality modality) {
        return new DegreeModalityResponse(
                modality.getId(), modality.getName(), modality.isActive());
    }
}

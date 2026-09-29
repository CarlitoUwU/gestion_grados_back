package com.claudecoders.grades.dataimport.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DataImportRequest(
        @NotBlank @Size(max = 255) String fileName,
        @NotNull Long importedById,
        @Min(0) int totalRecords,
        @Min(0) int importedRecords,
        @Min(0) int rejectedRecords,
        @Min(0) int observedRecords) {}

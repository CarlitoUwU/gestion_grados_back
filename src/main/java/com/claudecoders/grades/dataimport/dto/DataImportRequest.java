package com.claudecoders.grades.dataimport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DataImportRequest(
        @Schema(minLength = 1) @NotBlank @Size(min = 1, max = 255) String fileName,
        @NotNull Long importedById,
        @Min(0) int totalRecords,
        @Min(0) int importedRecords,
        @Min(0) int rejectedRecords,
        @Min(0) int observedRecords) {}

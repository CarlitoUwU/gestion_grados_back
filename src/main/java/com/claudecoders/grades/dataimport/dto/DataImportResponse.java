package com.claudecoders.grades.dataimport.dto;

import com.claudecoders.grades.dataimport.DataImport;
import java.time.LocalDateTime;

public record DataImportResponse(
        Long id,
        String fileName,
        Long importedById,
        int totalRecords,
        int importedRecords,
        int rejectedRecords,
        int observedRecords,
        String status,
        LocalDateTime importedAt) {
    public static DataImportResponse from(DataImport value) {
        return new DataImportResponse(
                value.getId(),
                value.getFileName(),
                value.getImportedBy().getId(),
                value.getTotalRecords(),
                value.getImportedRecords(),
                value.getRejectedRecords(),
                value.getObservedRecords(),
                value.getStatus(),
                value.getImportedAt());
    }
}

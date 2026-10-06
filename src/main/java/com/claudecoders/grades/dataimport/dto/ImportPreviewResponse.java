package com.claudecoders.grades.dataimport.dto;

import java.util.List;

public record ImportPreviewResponse(
        Long importId,
        String fileName,
        String status,
        int totalRecords,
        int validRecords,
        int rejectedRecords,
        int observedRecords,
        List<String> requiredColumns,
        List<ImportRowPreview> rows) {}

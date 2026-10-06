package com.claudecoders.grades.dataimport.dto;

import java.util.List;

public record ImportRowPreview(int rowNumber, String status, List<String> messages) {}

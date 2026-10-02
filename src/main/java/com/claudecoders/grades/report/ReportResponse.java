package com.claudecoders.grades.report;

import java.util.List;

public record ReportResponse(String title, List<String> columns, List<List<Object>> rows) {}

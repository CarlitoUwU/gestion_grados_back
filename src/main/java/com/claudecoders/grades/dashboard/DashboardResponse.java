package com.claudecoders.grades.dashboard;

import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long total,
        long active,
        long observed,
        long defended,
        Map<String, Long> statusDistribution,
        List<ExpedientApiModels.Summary> upcomingDefenses,
        List<ExpedientApiModels.Summary> recentExpedients) {}

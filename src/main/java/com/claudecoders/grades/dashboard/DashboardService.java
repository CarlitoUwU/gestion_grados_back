package com.claudecoders.grades.dashboard;

import com.claudecoders.grades.expedient.ExpedientQueryService;
import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final ExpedientQueryService expedients;

    public DashboardService(ExpedientQueryService expedients) {
        this.expedients = expedients;
    }

    public DashboardResponse dashboard(Integer year, Long schoolId) {
        var records = expedients.search(null, schoolId, null, null, year);
        LocalDate today = LocalDate.now();
        Map<String, Long> distribution =
                records.stream()
                        .collect(
                                Collectors.groupingBy(
                                        record -> value(record.statusName()),
                                        Collectors.counting()));
        long observed =
                records.stream()
                        .filter(
                                record ->
                                        value(record.statusName())
                                                .toLowerCase(Locale.ROOT)
                                                .contains("observ"))
                        .count();
        long defended =
                records.stream()
                        .filter(
                                record ->
                                        record.defenseDate() != null
                                                && !record.defenseDate().isAfter(today)
                                                && !"Pendiente"
                                                        .equalsIgnoreCase(
                                                                value(record.defenseResult())))
                        .count();
        long active =
                records.stream()
                        .filter(
                                record ->
                                        !value(record.statusName())
                                                .equalsIgnoreCase("Completado"))
                        .count();
        var upcoming =
                records.stream()
                        .filter(
                                record ->
                                        record.defenseDate() != null
                                                && !record.defenseDate().isBefore(today))
                        .sorted(Comparator.comparing(ExpedientApiModels.Summary::defenseDate))
                        .limit(5)
                        .toList();
        var recent =
                records.stream()
                        .sorted(
                                Comparator.comparing(
                                                ExpedientApiModels.Summary::updatedAt,
                                                Comparator.nullsLast(Comparator.naturalOrder()))
                                        .reversed())
                        .limit(8)
                        .toList();
        return new DashboardResponse(
                records.size(), active, observed, defended, distribution, upcoming, recent);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}

package com.claudecoders.grades.dashboard;

import com.claudecoders.grades.expedient.ExpedientQueryService;
import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import com.claudecoders.grades.shared.exception.BusinessValidationException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");
    private static final Set<String> ACTIVE_STATUSES =
            Set.of("registrado", "en tramite", "observado");
    static final Comparator<ExpedientApiModels.Summary> RECENT_ORDER =
            Comparator.comparing(
                            ExpedientApiModels.Summary::updatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(ExpedientApiModels.Summary::id, Comparator.reverseOrder());
    static final Comparator<ExpedientApiModels.Summary> UPCOMING_ORDER =
            Comparator.comparing(ExpedientApiModels.Summary::defenseDate)
                    .thenComparing(ExpedientApiModels.Summary::id);

    private final ExpedientQueryService expedients;
    private final Clock clock;

    public DashboardService(ExpedientQueryService expedients, Clock clock) {
        this.expedients = expedients;
        this.clock = clock;
    }

    public DashboardResponse dashboard(Integer year, Long schoolId) {
        validate(year, schoolId);
        var records = expedients.search(null, schoolId, null, null, year);
        LocalDate asOfDate = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        Map<String, Long> distribution =
                records.stream()
                        .collect(
                                Collectors.groupingBy(
                                        record -> value(record.statusName()),
                                        Collectors.counting()));
        long observed =
                records.stream()
                        .filter(record -> normalize(record.statusName()).contains("observ"))
                        .count();
        long defended =
                records.stream()
                        .filter(
                                record ->
                                        record.defenseDate() != null
                                                && !record.defenseDate().isAfter(asOfDate)
                                                && isDefended(record.defenseResult()))
                        .count();
        long active =
                records.stream()
                        .filter(record -> ACTIVE_STATUSES.contains(normalize(record.statusName())))
                        .count();
        List<ExpedientApiModels.Summary> upcoming =
                records.stream()
                        .filter(
                                record ->
                                        record.defenseDate() != null
                                                && !record.defenseDate().isBefore(asOfDate)
                                                && isPending(record.defenseResult()))
                        .sorted(UPCOMING_ORDER)
                        .limit(5)
                        .toList();
        List<ExpedientApiModels.Summary> recent =
                records.stream().sorted(RECENT_ORDER).limit(8).toList();
        return new DashboardResponse(
                records.size(),
                active,
                observed,
                defended,
                distribution,
                upcoming,
                recent,
                asOfDate,
                BUSINESS_ZONE.getId());
    }

    private static void validate(Integer year, Long schoolId) {
        if (year != null && (year < 1900 || year > 9999))
            throw new BusinessValidationException("year", "Debe estar entre 1900 y 9999");
        if (schoolId != null && schoolId <= 0)
            throw new BusinessValidationException("schoolId", "Debe ser un entero positivo");
    }

    private static boolean isDefended(String result) {
        return result != null && !result.isBlank() && !"pendiente".equals(normalize(result));
    }

    private static boolean isPending(String result) {
        return result == null || result.isBlank() || "pendiente".equals(normalize(result));
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}

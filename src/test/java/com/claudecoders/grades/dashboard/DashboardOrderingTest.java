package com.claudecoders.grades.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.claudecoders.grades.expedient.dto.ExpedientApiModels;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DashboardOrderingTest {
    @Test
    void recentExpedientsAreDescendingWithNullsLastAndIdTieBreak() {
        var newest = summary(1L, LocalDateTime.of(2026, 10, 31, 10, 0));
        var tiedLowerId = summary(2L, LocalDateTime.of(2026, 10, 31, 9, 0));
        var tiedHigherId = summary(9L, LocalDateTime.of(2026, 10, 31, 9, 0));
        var withoutUpdate = summary(20L, null);

        List<ExpedientApiModels.Summary> ordered =
                Stream.of(withoutUpdate, tiedLowerId, newest, tiedHigherId)
                        .sorted(DashboardService.RECENT_ORDER)
                        .toList();

        assertEquals(
                List.of(1L, 9L, 2L, 20L),
                ordered.stream().map(ExpedientApiModels.Summary::id).toList());
    }

    @Test
    void upcomingDefensesSortByDateThenAscendingId() {
        var later = summary(1L, LocalDateTime.of(2026, 10, 31, 10, 0), LocalDate.of(2026, 11, 2));
        var sameDayHigherId =
                summary(8L, LocalDateTime.of(2026, 10, 31, 10, 0), LocalDate.of(2026, 11, 1));
        var sameDayLowerId =
                summary(3L, LocalDateTime.of(2026, 10, 31, 10, 0), LocalDate.of(2026, 11, 1));

        List<ExpedientApiModels.Summary> ordered =
                Stream.of(later, sameDayHigherId, sameDayLowerId)
                        .sorted(DashboardService.UPCOMING_ORDER)
                        .toList();

        assertEquals(
                List.of(3L, 8L, 1L), ordered.stream().map(ExpedientApiModels.Summary::id).toList());
    }

    private static ExpedientApiModels.Summary summary(Long id, LocalDateTime updatedAt) {
        return summary(id, updatedAt, null);
    }

    private static ExpedientApiModels.Summary summary(
            Long id, LocalDateTime updatedAt, LocalDate defenseDate) {
        return new ExpedientApiModels.Summary(
                id,
                "EXP-" + id,
                LocalDate.of(2026, 1, 1),
                1L,
                "Graduado",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                defenseDate,
                null,
                updatedAt,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}

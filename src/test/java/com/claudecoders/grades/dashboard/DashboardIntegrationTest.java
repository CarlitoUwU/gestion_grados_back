package com.claudecoders.grades.dashboard;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claudecoders.grades.defense.Defense;
import com.claudecoders.grades.defense.DefenseRepository;
import com.claudecoders.grades.expedient.Expedient;
import com.claudecoders.grades.expedient.ExpedientRepository;
import com.claudecoders.grades.expedientstatus.ExpedientStatus;
import com.claudecoders.grades.expedientstatus.ExpedientStatusRepository;
import com.claudecoders.grades.graduate.Graduate;
import com.claudecoders.grades.graduate.GraduateRepository;
import com.claudecoders.grades.school.School;
import com.claudecoders.grades.school.SchoolRepository;
import com.claudecoders.grades.user.User;
import com.claudecoders.grades.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/keys")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@Import(DashboardIntegrationTest.FixedClockConfiguration.class)
class DashboardIntegrationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired SchoolRepository schools;
    @Autowired ExpedientStatusRepository statuses;
    @Autowired GraduateRepository graduates;
    @Autowired ExpedientRepository expedients;
    @Autowired DefenseRepository defenses;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        user = new User();
        user.setEmail("dashboard-" + suffix + "@unsa.edu.pe");
        user.setGoogleSubject("dashboard-subject-" + suffix);
        user.setFullName("Usuario dashboard " + suffix);
        user.setRole("ADMIN_GT");
        user = users.saveAndFlush(user);
    }

    @Test
    void emptyFilteredDashboardKeepsArraysCutoffAndTimeZone() throws Exception {
        School school = school("Escuela vacía");
        mvc.perform(
                        get("/api/v1/dashboard")
                                .param("schoolId", school.getId().toString())
                                .with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.active").value(0))
                .andExpect(jsonPath("$.observed").value(0))
                .andExpect(jsonPath("$.defended").value(0))
                .andExpect(jsonPath("$.statusDistribution").isEmpty())
                .andExpect(jsonPath("$.upcomingDefenses").isArray())
                .andExpect(jsonPath("$.upcomingDefenses.length()").value(0))
                .andExpect(jsonPath("$.recentExpedients").isArray())
                .andExpect(jsonPath("$.recentExpedients.length()").value(0))
                .andExpect(jsonPath("$.asOfDate").value("2026-10-31"))
                .andExpect(jsonPath("$.timeZone").value("America/Lima"));

        mvc.perform(
                        get("/api/v1/dashboard")
                                .param("year", "1900")
                                .param("schoolId", school.getId().toString())
                                .with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(
                        get("/api/v1/dashboard")
                                .param("year", "9999")
                                .param("schoolId", school.getId().toString())
                                .with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void filtersCountsDistributionAndOrderedListsFollowTheDashboardContract() throws Exception {
        int year = uniqueYear();
        School schoolA = school("Escuela A");
        School schoolB = school("Escuela B");
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        var e1 = expedient(year, schoolA, "Registrado", TODAY.minusDays(1), "Aprobado");
        var e2 = expedient(year, schoolA, "En trámite", TODAY, null);
        var e3 = expedient(year, schoolA, "Observado", TODAY.plusDays(1), "Pendiente");
        var e4 = expedient(year, schoolA, "Sustentado", TODAY.minusDays(1), " ");
        var e5 = expedient(year, schoolA, "Completado", TODAY.minusDays(1), "Pendiente");
        var e6 = expedient(year, schoolA, "En revisión " + suffix, TODAY.plusDays(2), "Pendiente");
        var e7 = expedient(year, schoolA, "Registrado", null, null);
        var e8 = expedient(year, schoolA, "Registrado", TODAY, "Pendiente");
        var e9 = expedient(year, schoolB, "Registrado", TODAY, "Pendiente");
        var e10 = expedient(year, schoolA, "Registrado", TODAY.plusDays(1), "Pendiente");
        var e11 = expedient(year, schoolA, "Registrado", TODAY.plusDays(3), "Pendiente");

        mvc.perform(
                        get("/api/v1/dashboard")
                                .param("year", Integer.toString(year))
                                .param("schoolId", schoolA.getId().toString())
                                .with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.active").value(7))
                .andExpect(jsonPath("$.observed").value(1))
                .andExpect(jsonPath("$.defended").value(1))
                .andExpect(jsonPath("$.statusDistribution.Registrado").value(5))
                .andExpect(jsonPath("$.statusDistribution['En trámite']").value(1))
                .andExpect(jsonPath("$.statusDistribution.Observado").value(1))
                .andExpect(jsonPath("$.statusDistribution.Sustentado").value(1))
                .andExpect(jsonPath("$.statusDistribution.Completado").value(1))
                .andExpect(jsonPath("$.statusDistribution['En revisión " + suffix + "']").value(1))
                .andExpect(jsonPath("$.upcomingDefenses.length()").value(5))
                .andExpect(jsonPath("$.upcomingDefenses[0].id").value(e2.getId()))
                .andExpect(jsonPath("$.upcomingDefenses[1].id").value(e8.getId()))
                .andExpect(jsonPath("$.upcomingDefenses[2].id").value(e3.getId()))
                .andExpect(jsonPath("$.upcomingDefenses[3].id").value(e10.getId()))
                .andExpect(jsonPath("$.upcomingDefenses[4].id").value(e6.getId()))
                .andExpect(jsonPath("$.asOfDate").value(TODAY.toString()))
                .andExpect(jsonPath("$.timeZone").value("America/Lima"));

        mvc.perform(
                        get("/api/v1/dashboard")
                                .param("year", Integer.toString(year))
                                .with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(11));
        mvc.perform(
                        get("/api/v1/dashboard")
                                .param("schoolId", schoolB.getId().toString())
                                .with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.recentExpedients[0].id").value(e9.getId()));

        List<Long> ids =
                new ArrayList<>(
                        List.of(
                                e1.getId(),
                                e2.getId(),
                                e3.getId(),
                                e4.getId(),
                                e5.getId(),
                                e6.getId(),
                                e7.getId(),
                                e8.getId(),
                                e10.getId(),
                                e11.getId()));
        LocalDateTime tiedUpdate = LocalDateTime.of(2026, 10, 31, 10, 0);
        for (Long id : ids) {
            jdbc.update(
                    "update expedients set updated_at = ? where id = ?",
                    Timestamp.valueOf(tiedUpdate),
                    id);
        }
        entityManager.clear();
        ids.sort(Comparator.reverseOrder());
        List<Long> expectedRecent = ids.subList(0, 8);

        mvc.perform(
                        get("/api/v1/dashboard")
                                .param("year", Integer.toString(year))
                                .param("schoolId", schoolA.getId().toString())
                                .with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentExpedients.length()").value(8))
                .andExpect(jsonPath("$.recentExpedients[0].id").value(expectedRecent.get(0)))
                .andExpect(jsonPath("$.recentExpedients[1].id").value(expectedRecent.get(1)))
                .andExpect(jsonPath("$.recentExpedients[7].id").value(expectedRecent.get(7)));
    }

    @Test
    void invalidFiltersReturn400WithFieldErrors() throws Exception {
        mvc.perform(get("/api/v1/dashboard").param("year", "1899").with(jwtFor(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.year").exists());
        mvc.perform(get("/api/v1/dashboard").param("year", "10000").with(jwtFor(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.year").exists());
        mvc.perform(get("/api/v1/dashboard").param("schoolId", "0").with(jwtFor(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.schoolId").exists());
        mvc.perform(get("/api/v1/dashboard").param("schoolId", "-3").with(jwtFor(user)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/dashboard").param("year", "not-a-year").with(jwtFor(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.year").exists());
    }

    private int uniqueYear() {
        Integer year =
                jdbc.queryForObject(
                        """
                        select y from generate_series(1900, 9999) y
                        where not exists (
                            select 1 from expedients e
                            where extract(year from e.start_date) = y
                        )
                        order by y limit 1
                        """,
                        Integer.class);
        if (year == null) throw new IllegalStateException("No hay año libre para la prueba");
        return year;
    }

    private School school(String prefix) {
        School school = new School();
        school.setName(prefix + " " + UUID.randomUUID());
        return schools.saveAndFlush(school);
    }

    private ExpedientStatus expedientStatus(String name) {
        return statuses.findAll().stream()
                .filter(status -> status.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(
                        () -> {
                            ExpedientStatus status = new ExpedientStatus();
                            status.setName(name);
                            return statuses.saveAndFlush(status);
                        });
    }

    private Expedient expedient(
            int year,
            School school,
            String statusName,
            LocalDate defenseDate,
            String defenseResult) {
        String suffix = UUID.randomUUID().toString();
        Graduate graduate = new Graduate();
        graduate.setFirstNames("Nombre " + suffix);
        graduate.setLastNames("Apellido");
        graduate.setDocumentNumber("DNI-" + suffix.substring(0, 12));
        graduate.setSchool(school);
        graduate = graduates.saveAndFlush(graduate);

        Expedient expedient = new Expedient();
        expedient.setNumber("EXP-" + suffix);
        expedient.setStartDate(LocalDate.of(year, 1, 15));
        expedient.setGraduate(graduate);
        expedient.setStatus(expedientStatus(statusName));
        expedient = expedients.saveAndFlush(expedient);

        if (defenseDate != null || defenseResult != null) {
            Defense defense = new Defense();
            defense.setExpedient(expedient);
            defense.setDefenseDate(defenseDate);
            defense.setResult(defenseResult);
            defenses.saveAndFlush(defense);
        }
        return expedient;
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(
            User user) {
        return jwt().jwt(token -> token.subject(user.getGoogleSubject()));
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedBusinessClock() {
            return Clock.fixed(Instant.parse("2026-10-31T15:00:00Z"), ZoneId.of("America/Lima"));
        }
    }
}

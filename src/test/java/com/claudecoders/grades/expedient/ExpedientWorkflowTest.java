package com.claudecoders.grades.expedient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.claudecoders.grades.expedient.dto.ExpedientProcessRequests;
import com.claudecoders.grades.expedient.dto.ExpedientWriteRequest;
import com.claudecoders.grades.expedientstatus.ExpedientStatus;
import com.claudecoders.grades.expedientstatus.ExpedientStatusRepository;
import com.claudecoders.grades.graduate.GraduateRepository;
import com.claudecoders.grades.researchwork.ResearchWorkRepository;
import com.claudecoders.grades.shared.exception.ResourceNotFoundException;
import com.claudecoders.grades.teacher.Teacher;
import com.claudecoders.grades.teacher.TeacherRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@ActiveProfiles("test")
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:workflow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.flyway.enabled=false",
            "spring.docker.compose.enabled=false",
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/jwks"
        })
class ExpedientWorkflowTest {
    @Autowired ExpedientService service;
    @Autowired ExpedientProcessService processes;
    @Autowired ExpedientQueryService queries;
    @Autowired ExpedientRepository expedients;
    @Autowired GraduateRepository graduates;
    @Autowired ResearchWorkRepository research;
    @Autowired TeacherRepository teachers;
    @Autowired ExpedientStatusRepository statuses;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        for (String table :
                List.of(
                        "jury_draw_members",
                        "jury_draws",
                        "jury_members",
                        "expedient_advisors",
                        "defenses",
                        "resolutions",
                        "research_works",
                        "expedients",
                        "graduates",
                        "teachers",
                        "expedient_statuses")) {
            jdbc.update("delete from " + table);
        }
    }

    @Test
    void createsAggregateReusesGraduateAndUpdatesExistingResearch() {
        var created = service.create(request("EXP-2026-001", "Initial title", null), null);
        var second = service.create(request("EXP-2026-002", "Other title", null), null);
        assertThat(second.graduate().id()).isEqualTo(created.graduate().id());
        assertThat(graduates.count()).isEqualTo(1);
        var updated =
                service.update(
                        "EXP-2026-001", request("EXP-2026-001", "Updated title", null), null);
        assertThat(updated.research().id()).isEqualTo(created.research().id());
        assertThat(updated.research().title()).isEqualTo("Updated title");
        assertThat(research.count()).isEqualTo(2);
        assertThat(updated.graduate().fullName()).isEqualTo("Sofia Valdivia Paredes");
        assertThat(updated.graduate().studentCode()).isEqualTo("20193456");
    }

    @Test
    void reusingLegacyGraduatePreservesKnownNameParts() {
        var graduate = new com.claudecoders.grades.graduate.Graduate();
        graduate.setFirstNames("Sofia");
        graduate.setLastNames("Valdivia Paredes");
        graduate.setDocumentNumber("72345678");
        graduate = graduates.save(graduate);
        var created = service.create(request("EXP-2026-001", "Title", null), null);
        assertThat(created.graduate().id()).isEqualTo(graduate.getId());
        var persisted = graduates.findById(graduate.getId()).orElseThrow();
        assertThat(persisted.getFirstNames()).isEqualTo("Sofia");
        assertThat(persisted.getLastNames()).isEqualTo("Valdivia Paredes");
    }

    @Test
    void failedReferenceRollsBackExpedientAndGraduateEdits() {
        var created = service.create(request("EXP-2026-001", "Original title", null), null);
        assertThatThrownBy(
                        () ->
                                service.update(
                                        "EXP-2026-001",
                                        request("EXP-CHANGED", "Changed title", Long.MAX_VALUE),
                                        null))
                .isInstanceOf(ResourceNotFoundException.class);
        var unchanged = queries.detail("EXP-2026-001");
        assertThat(unchanged.summary().number()).isEqualTo("EXP-2026-001");
        assertThat(unchanged.research().id()).isEqualTo(created.research().id());
        assertThat(unchanged.research().title()).isEqualTo("Original title");
        assertThatThrownBy(
                        () ->
                                service.create(
                                        request("EXP-FAILED", "Invalid", Long.MAX_VALUE), null))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(expedients.count()).isEqualTo(1);
        assertThat(graduates.count()).isEqualTo(1);
    }

    @Test
    void searchesFullNamesAndReturnsDistinctDatabasePages() {
        service.create(request("HIST-2026-001", "Title", null), null);
        service.create(request("HIST-2026-002", "Title", null), null);
        var first = queries.search("Sofia Valdivia", null, null, null, 2026, 0, 1);
        var second = queries.search("Sofia Valdivia", null, null, null, 2026, 1, 1);
        assertThat(first.totalElements()).isEqualTo(2);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.content().getFirst().number())
                .isNotEqualTo(second.content().getFirst().number());
        assertThat(queries.detail("hist-2026-001").summary().number()).isEqualTo("HIST-2026-001");
        assertThat(queries.search(null, null, null, null, 2025, 0, 20).totalElements()).isZero();
    }

    @Test
    void academicUpsertsReuseResolutionsAndInvalidJuryPreservesAssignments() {
        service.create(request("EXP-2026-001", "Title", null), null);
        List<Long> ids = List.of(teacher("One"), teacher("Two"), teacher("Three"));
        var advisorRequest =
                new ExpedientProcessRequests.Advisor(
                        ids.get(0), "AS-001", LocalDate.of(2026, 4, 10));
        var advisor = processes.advisor("EXP-2026-001", advisorRequest, null).advisor();
        var repeated = processes.advisor("EXP-2026-001", advisorRequest, null).advisor();
        assertThat(repeated.id()).isEqualTo(advisor.id());
        assertThat(repeated.resolutionId()).isEqualTo(advisor.resolutionId());

        var members =
                List.of(
                        new ExpedientProcessRequests.JuryMemberInput(ids.get(0), "Presidente"),
                        new ExpedientProcessRequests.JuryMemberInput(ids.get(1), "Secretario"),
                        new ExpedientProcessRequests.JuryMemberInput(ids.get(2), "Vocal"));
        processes.jury("EXP-2026-001", new ExpedientProcessRequests.Jury("JR-001", members), null);
        var jury =
                processes
                        .jury(
                                "EXP-2026-001",
                                new ExpedientProcessRequests.Jury("JR-001", members),
                                null)
                        .jury();
        assertThat(jury).hasSize(3);
        var duplicated =
                List.of(
                        members.get(0),
                        new ExpedientProcessRequests.JuryMemberInput(ids.get(0), "Secretario"),
                        members.get(2));
        assertThatThrownBy(
                        () ->
                                processes.jury(
                                        "EXP-2026-001",
                                        new ExpedientProcessRequests.Jury("INVALID", duplicated),
                                        null))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(queries.detail("EXP-2026-001").jury()).isEqualTo(jury);
        assertThat(queries.detail("EXP-2026-001").resolutions())
                .noneMatch(r -> r.number().equals("INVALID"));

        var defenseRequest =
                new ExpedientProcessRequests.Defense(LocalDate.of(2026, 6, 15), "Aprobado");
        var defense = processes.defense("EXP-2026-001", defenseRequest, null).defense();
        assertThat(processes.defense("EXP-2026-001", defenseRequest, null).defense().id())
                .isEqualTo(defense.id());
        var status = new ExpedientStatus();
        status.setName("Observado");
        status = statuses.save(status);
        var result =
                processes.status(
                        "EXP-2026-001",
                        new ExpedientProcessRequests.Status("observado", "Falta constancia"),
                        null);
        assertThat(result.summary().statusId()).isEqualTo(status.getId());
        assertThat(result.observations()).isEqualTo("Falta constancia");
    }

    private Long teacher(String name) {
        Teacher teacher = new Teacher();
        teacher.setFullName(name);
        return teachers.save(teacher).getId();
    }

    private ExpedientWriteRequest request(String number, String title, Long schoolId) {
        return new ExpedientWriteRequest(
                number,
                LocalDate.of(2026, 3, 15),
                "Título profesional",
                null,
                null,
                "Ingreso inicial",
                new ExpedientWriteRequest.GraduateInput(
                        "Sofia Valdivia Paredes", "72345678", "20193456", schoolId, "Pregrado"),
                new ExpedientWriteRequest.ResearchInput(title));
    }
}

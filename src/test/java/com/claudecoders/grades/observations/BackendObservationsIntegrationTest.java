package com.claudecoders.grades.observations;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.claudecoders.grades.expedient.Expedient;
import com.claudecoders.grades.expedient.ExpedientRepository;
import com.claudecoders.grades.expedientadvisor.ExpedientAdvisor;
import com.claudecoders.grades.expedientadvisor.ExpedientAdvisorRepository;
import com.claudecoders.grades.expedientstatus.*;
import com.claudecoders.grades.graduate.*;
import com.claudecoders.grades.jurymember.*;
import com.claudecoders.grades.modality.*;
import com.claudecoders.grades.resolution.*;
import com.claudecoders.grades.school.*;
import com.claudecoders.grades.teacher.*;
import com.claudecoders.grades.user.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        properties = {
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/keys",
            "logging.level.org.hibernate.orm.jdbc.bind=OFF",
            "logging.level.org.hibernate.SQL=OFF"
        })
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class BackendObservationsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;
    @Autowired UserRepository users;
    @Autowired SchoolRepository schools;
    @Autowired DegreeModalityRepository modalities;
    @Autowired ExpedientStatusRepository statuses;
    @Autowired ExpedientRepository expedients;
    @Autowired GraduateRepository graduates;
    @Autowired TeacherRepository teachers;
    @Autowired ExpedientAdvisorRepository advisors;
    @Autowired JuryMemberRepository juries;
    @Autowired ResolutionRepository resolutions;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired com.claudecoders.grades.jurydraw.JuryDrawRepository draws;
    @Autowired com.claudecoders.grades.jurydrawmember.JuryDrawMemberRepository drawMembers;
    User user;
    School school;
    DegreeModality modality;
    ExpedientStatus state;
    String suffix;

    @BeforeEach
    void setup() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        user = new User();
        user.setEmail(suffix + "@unsa.edu.pe");
        user.setFullName("Operador");
        user.setGoogleSubject(suffix);
        user = users.saveAndFlush(user);
        school = new School();
        school.setName("Escuela " + suffix);
        school = schools.saveAndFlush(school);
        modality = new DegreeModality();
        modality.setName("Modalidad " + suffix);
        modality = modalities.saveAndFlush(modality);
        state = new ExpedientStatus();
        state.setName("Registrado " + suffix);
        state = statuses.saveAndFlush(state);
    }

    RequestPostProcessor auth() {
        return jwt().jwt(j -> j.subject(user.getGoogleSubject()));
    }

    JsonNode json(String body) {
        return mapper.readTree(body);
    }

    JsonNode complete(String number) throws Exception {
        String body =
                """
                {"number":"%s","startDate":"2025-01-01","modalityId":%d,"statusId":%d,
                "graduate":{"documentNumber":"%s","firstNames":"Lucía","lastNames":"Paredes","schoolId":%d},
                "research":{"title":"Optimización %s"}}
                """
                        .formatted(
                                number,
                                modality.getId(),
                                state.getId(),
                                suffix,
                                school.getId(),
                                suffix);
        return json(
                mvc.perform(
                                post("/api/v1/expedients/complete")
                                        .with(auth())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8));
    }

    Expedient expediente(String number) throws Exception {
        return expedients
                .findById(complete(number).path("summary").path("id").asLong())
                .orElseThrow();
    }

    Teacher teacher() {
        Teacher t = new Teacher();
        t.setFullName("Docente " + UUID.randomUUID());
        return teachers.saveAndFlush(t);
    }

    ExpedientAdvisor advisor(Expedient e, Teacher t, int year) {
        var a = new ExpedientAdvisor();
        a.setExpedient(e);
        a.setTeacher(t);
        a.setAssignedAt(LocalDate.of(year, 2, 1));
        return advisors.saveAndFlush(a);
    }

    Resolution resolution(Expedient e) {
        var r = new Resolution();
        r.setExpedient(e);
        r.setNumber("RES-" + suffix);
        return resolutions.saveAndFlush(r);
    }

    @Test
    void completeReusesGraduateAndDuplicateNumberHasExactConflict() throws Exception {
        var first = complete("A-" + suffix);
        var second = complete("B-" + suffix);
        assertEquals(first.path("graduate").path("id"), second.path("graduate").path("id"));
        assertEquals(
                1,
                graduates.findAll().stream()
                        .filter(g -> suffix.equals(g.getDocumentNumber()))
                        .count());
        mvc.perform(
                        post("/api/v1/expedients/complete")
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"number":"A-%s","startDate":"2025-01-01","modalityId":%d,"statusId":%d,
                "graduate":{"firstNames":"Otro","lastNames":"Nombre","schoolId":%d},"research":{"title":"Otro"}}
                """
                                                .formatted(
                                                        suffix,
                                                        modality.getId(),
                                                        state.getId(),
                                                        school.getId())))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message").value("El número de expediente ya está registrado"));
    }

    @Test
    @Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentCompleteRequestsReuseOneGraduate() throws Exception {
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var ready = new java.util.concurrent.CountDownLatch(2);
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<JsonNode> first =
                    () -> {
                        ready.countDown();
                        start.await();
                        return complete("C-" + suffix);
                    };
            java.util.concurrent.Callable<JsonNode> second =
                    () -> {
                        ready.countDown();
                        start.await();
                        return complete("D-" + suffix);
                    };
            var a = pool.submit(first);
            var b = pool.submit(second);
            boolean bothReady = ready.await(10, java.util.concurrent.TimeUnit.SECONDS);
            start.countDown();
            assertTrue(bothReady);
            assertEquals(
                    a.get(30, java.util.concurrent.TimeUnit.SECONDS).path("graduate").path("id"),
                    b.get(30, java.util.concurrent.TimeUnit.SECONDS).path("graduate").path("id"));
        }
        assertEquals(
                1,
                graduates.findAll().stream()
                        .filter(g -> suffix.equals(g.getDocumentNumber()))
                        .count());
    }

    @Test
    void searchAcceptsMissingEmptyAndWhitespaceAndIgnoresAccents() throws Exception {
        complete("EXP-" + suffix);
        for (String term :
                new String[] {
                    "", "   ", "LUCIA", "lucia paredes", "OPTIMIZACION", suffix, "EXP-" + suffix
                }) {
            mvc.perform(
                            get("/api/v1/expedients/search")
                                    .with(auth())
                                    .param("schoolId", school.getId().toString())
                                    .param("search", term))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
        mvc.perform(
                        get("/api/v1/expedients/search")
                                .with(auth())
                                .param("schoolId", school.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(
                        get("/api/v1/expedients/search")
                                .with(auth())
                                .param("search", "NO-EXISTE-" + suffix))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void missingTeachersReturnApiErrorAndEmptyHistoryIsArray() throws Exception {
        for (String tail : new String[] {"", "/history"}) {
            mvc.perform(get("/api/v1/teachers/" + Long.MAX_VALUE + tail).with(auth()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Docente no encontrado"));
        }
        var t = teacher();
        mvc.perform(get("/api/v1/teachers/{id}/history", t.getId()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void historyKeepsOnlyCurrentDesignationsAndExplicitNullResolution() throws Exception {
        var e = expediente("EXP-" + suffix);
        var old = teacher();
        var current = teacher();
        advisor(e, old, 2025);
        advisor(e, current, 2026);
        mvc.perform(get("/api/v1/teachers/{id}/history", old.getId()).with(auth()))
                .andExpect(content().json("[]"));
        var rows =
                json(
                        mvc.perform(
                                        get("/api/v1/teachers/{id}/history", current.getId())
                                                .with(auth()))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(StandardCharsets.UTF_8));
        assertEquals(1, rows.size());
        for (String field : new String[] {"resolutionId", "resolutionNumber", "resolutionDate"}) {
            assertTrue(rows.get(0).has(field));
            assertTrue(rows.get(0).path(field).isNull());
        }
    }

    @Test
    void teacherReportUsesParticipationYearAndSupportsOptionalTeacher() throws Exception {
        var first = expediente("A-" + suffix);
        var second = expediente("B-" + suffix);
        var t = teacher();
        advisor(first, t, 2025);
        advisor(second, t, 2026);
        for (int year : new int[] {2025, 2026}) {
            mvc.perform(
                            get("/api/v1/reports/teachers")
                                    .with(auth())
                                    .param("teacherId", t.getId().toString())
                                    .param("year", Integer.toString(year)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rows.length()").value(1))
                    .andExpect(
                            jsonPath("$.rows[0][0]")
                                    .value(
                                            (year == 2025 ? "A-" : "B-")
                                                    + suffix.toUpperCase(java.util.Locale.ROOT)));
        }
        mvc.perform(get("/api/v1/reports/teachers").with(auth()).param("year", "2026"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/reports/teachers").with(auth()).param("year", "99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pdfUploadDownloadReplacementAndValidation() throws Exception {
        var r = resolution(expediente("EXP-" + suffix));
        String path = "/api/v1/resolutions/" + r.getId() + "/document";
        mvc.perform(get(path).with(auth())).andExpect(status().isNotFound());
        byte[] pdf = "%PDF-1.7\nQA document\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        mvc.perform(
                        multipart(path)
                                .file(
                                        new MockMultipartFile(
                                                "file", "res.pdf", "application/pdf", pdf))
                                .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileUrl").value("http://localhost" + path));
        entityManager.clear();
        mvc.perform(get(path).with(auth()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdf));
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(multipart(path).with(auth())).andExpect(status().isBadRequest());
        mvc.perform(
                        multipart(path)
                                .file(new MockMultipartFile("file", "x.txt", "text/plain", pdf))
                                .with(auth()))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        multipart(path)
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "x.pdf",
                                                "application/pdf",
                                                new byte[] {1, 2, 3}))
                                .with(auth()))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        multipart(path)
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "x.pdf",
                                                "application/pdf",
                                                new byte[10 * 1024 * 1024 + 1]))
                                .with(auth()))
                .andExpect(status().is(413));
        mvc.perform(
                        multipart("/api/v1/resolutions/{id}/document", Long.MAX_VALUE)
                                .file(
                                        new MockMultipartFile(
                                                "file", "x.pdf", "application/pdf", pdf))
                                .with(auth()))
                .andExpect(status().isNotFound());
        mvc.perform(get(path).with(auth()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdf));
        byte[] replacement = "%PDF-2.0\nReplacement\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        mvc.perform(
                        multipart(path)
                                .file(
                                        new MockMultipartFile(
                                                "file", "res.pdf", "application/pdf", replacement))
                                .with(auth()))
                .andExpect(status().isOk());
        mvc.perform(get(path).with(auth())).andExpect(content().bytes(replacement));
    }

    @Test
    void juryHistoryExcludesDrawsAndSupersededRolesAndReportUsesResolutionYear() throws Exception {
        var e = expediente("EXP-" + suffix);
        var former = teacher();
        var current = teacher();
        var drawOnly = teacher();
        var old = new JuryMember();
        old.setExpedient(e);
        old.setTeacher(former);
        old.setJuryRole(JuryRole.VOCAL);
        juries.saveAndFlush(old);
        var r = resolution(e);
        r.setResolutionDate(LocalDate.of(2024, 3, 1));
        resolutions.saveAndFlush(r);
        var active = new JuryMember();
        active.setExpedient(e);
        active.setTeacher(current);
        active.setJuryRole(JuryRole.VOCAL);
        active.setResolution(r);
        juries.saveAndFlush(active);
        mvc.perform(get("/api/v1/teachers/{id}/history", former.getId()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mvc.perform(get("/api/v1/teachers/{id}/history", current.getId()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resolutionId").value(r.getId()));
        mvc.perform(
                        get("/api/v1/reports/teachers")
                                .with(auth())
                                .param("teacherId", current.getId().toString())
                                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(1));
        mvc.perform(
                        get("/api/v1/reports/teachers")
                                .with(auth())
                                .param("teacherId", current.getId().toString())
                                .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(0));
        var draw = new com.claudecoders.grades.jurydraw.JuryDraw();
        draw.setExpedient(e);
        draw.setDrawDate(LocalDate.of(2025, 2, 1));
        draw = draws.saveAndFlush(draw);
        var member = new com.claudecoders.grades.jurydrawmember.JuryDrawMember();
        member.setJuryDraw(draw);
        member.setTeacher(drawOnly);
        drawMembers.saveAndFlush(member);
        mvc.perform(get("/api/v1/teachers/{id}/history", drawOnly.getId()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void acceptsTenMiBAndCatalogsKeepTeacherContract() throws Exception {
        teacher();
        for (String route : new String[] {"/api/v1/teachers", "/api/v1/catalogs/teachers"}) {
            var rows =
                    json(
                            mvc.perform(get(route).with(auth()))
                                    .andExpect(status().isOk())
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString(StandardCharsets.UTF_8));
            assertTrue(rows.isArray());
            for (var row : rows) {
                assertEquals(4, row.size());
                assertTrue(row.has("id"));
                assertTrue(row.has("documentNumber"));
                assertTrue(row.has("fullName"));
                assertTrue(row.has("email"));
                assertFalse(row.has("schoolId"));
                assertFalse(row.has("schoolName"));
            }
        }
        var r = resolution(expediente("EXP-" + suffix));
        byte[] data = new byte[10 * 1024 * 1024];
        System.arraycopy("%PDF-1.7".getBytes(StandardCharsets.US_ASCII), 0, data, 0, 8);
        mvc.perform(
                        multipart("/api/v1/resolutions/{id}/document", r.getId())
                                .file(
                                        new MockMultipartFile(
                                                "file", "limit.pdf", "application/pdf", data))
                                .with(auth()))
                .andExpect(status().isOk());
    }

    @Test
    void openApiDocumentsCreatedRolesAndNonEmptyStrings() throws Exception {
        var doc =
                json(
                        mvc.perform(get("/api/docs/openapi.json"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(StandardCharsets.UTF_8));
        for (String path : new String[] {"/api/v1/expedients/complete", "/api/v1/accesses"}) {
            var responses = doc.path("paths").path(path).path("post").path("responses");
            assertTrue(responses.has("201"));
            assertFalse(responses.has("200"));
        }
        var schemas = doc.path("components").path("schemas");
        assertEquals(
                4, schemas.path("JuryInput").path("properties").path("role").path("enum").size());
        assertEquals(
                1,
                schemas.path("CompleteExpedientRequest")
                        .path("properties")
                        .path("number")
                        .path("minLength")
                        .asInt());
        assertEquals(
                1,
                schemas.path("ResolutionRequest")
                        .path("properties")
                        .path("number")
                        .path("minLength")
                        .asInt());
        assertTrue(
                doc.path("paths")
                        .path("/api/v1/resolutions/{id}/document")
                        .path("post")
                        .path("responses")
                        .has("413"));
    }

    @Autowired com.claudecoders.grades.defense.DefenseRepository defenses;

    void defense(Expedient e, LocalDate date, String result) {
        var d = new com.claudecoders.grades.defense.Defense();
        d.setExpedient(e);
        d.setDefenseDate(date);
        d.setResult(result);
        defenses.saveAndFlush(d);
    }

    @Test
    void reportsIdentifyTeacherAndExcludeFutureOrPendingDefenses() throws Exception {
        var t = teacher();
        advisor(expediente("T-" + suffix), t, 2025);
        var report =
                json(
                        mvc.perform(
                                        get("/api/v1/reports/teachers")
                                                .with(auth())
                                                .param("year", "2025"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.columns[7]").value("ID docente"))
                                .andExpect(jsonPath("$.columns[8]").value("Docente"))
                                .andReturn()
                                .getResponse()
                                .getContentAsString(StandardCharsets.UTF_8));
        String number = "T-" + suffix.toUpperCase(java.util.Locale.ROOT);
        var row = new java.util.ArrayList<JsonNode>();
        report.path("rows")
                .forEach(
                        r -> {
                            if (number.equals(r.path(0).asString())) row.add(r);
                        });
        assertEquals(1, row.size());
        assertEquals(t.getId(), row.get(0).path(7).asLong());
        assertEquals(t.getFullName(), row.get(0).path(8).asString());
        mvc.perform(get("/api/v1/reports/teachers").with(auth()).param("teacherId", "999999999"))
                .andExpect(status().isNotFound());

        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Lima"));
        defense(expediente("D1-" + suffix), today.minusDays(1), "Aprobado");
        defense(expediente("D2-" + suffix), today.plusDays(10), "Aprobado");
        defense(expediente("D3-" + suffix), today.minusDays(2), "pendiente");
        defense(expediente("D4-" + suffix), today.minusDays(3), " ");
        mvc.perform(
                        get("/api/v1/reports/defended-works")
                                .with(auth())
                                .param("schoolId", school.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(1))
                .andExpect(
                        jsonPath("$.rows[0][2]")
                                .value("D1-" + suffix.toUpperCase(java.util.Locale.ROOT)));
        mvc.perform(
                        get("/api/v1/reports/statistics")
                                .with(auth())
                                .param("schoolId", school.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[6]").value("Sustentación"))
                .andExpect(jsonPath("$.columns[7]").value("Resultado"))
                .andExpect(jsonPath("$.rows.length()").value(5));
        for (String route : new String[] {"statistics", "draws", "defended-works"}) {
            mvc.perform(get("/api/v1/reports/" + route).with(auth()).param("year", "99"))
                    .andExpect(status().isBadRequest());
            mvc.perform(get("/api/v1/reports/" + route).with(auth()).param("schoolId", "0"))
                    .andExpect(status().isBadRequest());
        }
    }

    String etag(long id) throws Exception {
        String etag =
                mvc.perform(get("/api/v1/expedients/{id}/detail", id).with(auth()))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getHeader("ETag");
        assertNotNull(etag);
        return etag;
    }

    org.springframework.test.web.servlet.ResultActions putAcademic(
            long id, String ifMatch, String resolution) throws Exception {
        var request =
                put("/api/v1/expedients/{id}/academic", id)
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"resolutions":[{"clientId":"r1","number":"%s"}],
                                 "advisor":null,"jury":null,"draws":[],"defense":null}
                                """
                                        .formatted(resolution));
        if (ifMatch != null) request.header("If-Match", ifMatch);
        return mvc.perform(request);
    }

    @Test
    void academicReplacementHonorsIfMatchVersion() throws Exception {
        long id = complete("V-" + suffix).path("summary").path("id").asLong();
        String first = etag(id);
        assertEquals(first, etag(id));

        putAcademic(id, first, "RV1-" + suffix).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        String second = etag(id);
        assertNotEquals(first, second);

        putAcademic(id, first, "RV2-" + suffix)
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.status").value(412))
                .andExpect(
                        jsonPath("$.message")
                                .value(org.hamcrest.Matchers.containsString("cambió")));
        entityManager.clear();
        mvc.perform(get("/api/v1/expedients/{id}/detail", id).with(auth()))
                .andExpect(jsonPath("$.resolutions[0].number").value("RV1-" + suffix));

        putAcademic(id, "*", "RV3-" + suffix).andExpect(status().isOk());
        putAcademic(id, null, "RV4-" + suffix)
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.status").value(428));
        putAcademic(999_999_999L, second, "RV5-" + suffix).andExpect(status().isNotFound());
    }
}

package com.claudecoders.grades.dataimport;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.claudecoders.grades.user.User;
import com.claudecoders.grades.user.UserRepository;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/keys")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AdministrationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    String suffix;
    User owner;
    User other;

    @BeforeEach
    void setup() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        owner = user("owner");
        other = user("other");
    }

    User user(String name) {
        User value = new User();
        value.setEmail("adm-" + name + "-" + suffix + "@unsa.edu.pe");
        value.setFullName("Administrativo sintético " + name);
        value.setGoogleSubject("adm-" + name + "-" + suffix);
        value.setRole("ADMIN_GT");
        value.setActive(true);
        return users.save(value);
    }

    @AfterEach
    void cleanup() {
        jdbc.update(
                "delete from research_works where expedient_id in (select id from expedients where created_by = ?)",
                owner.getId());
        jdbc.update("delete from expedients where created_by = ?", owner.getId());
        jdbc.update("delete from graduates where document_number like ?", "ADM-" + suffix + "%");
        jdbc.update(
                "delete from data_imports where imported_by in (?, ?)",
                owner.getId(),
                other.getId());
        jdbc.update("delete from users where email like ?", "adm-%-" + suffix + "@unsa.edu.pe");
    }

    RequestPostProcessor auth(User user) {
        return jwt().jwt(j -> j.subject(user.getGoogleSubject()));
    }

    byte[] workbook(boolean namesColumn, String[][] rows) throws Exception {
        try (var book = new XSSFWorkbook();
                var output = new ByteArrayOutputStream()) {
            var sheet = book.createSheet("Expedientes");
            String[] headers = {
                "numero_expediente",
                "fecha_inicio",
                namesColumn ? "nombres" : "columna_invalida",
                "apellidos",
                "titulo_trabajo",
                "documento",
                "escuela"
            };
            var header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            for (int i = 0; i < rows.length; i++) {
                var row = sheet.createRow(i + 1);
                for (int j = 0; j < rows[i].length; j++) row.createCell(j).setCellValue(rows[i][j]);
            }
            book.write(output);
            return output.toByteArray();
        }
    }

    String[] row(String name) {
        return new String[] {
            "ADM-" + suffix + "-" + name,
            "2025-01-01",
            "Persona",
            "Sintética",
            "Trabajo de prueba",
            "ADM-" + suffix + name,
            ""
        };
    }

    JsonNode preview(String[][] rows) throws Exception {
        return mapper.readTree(
                mvc.perform(
                                multipart("/api/v1/data-imports/preview")
                                        .file(
                                                new MockMultipartFile(
                                                        "file",
                                                        "historico.xlsx",
                                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                        workbook(true, rows)))
                                        .with(auth(owner)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
    }

    long count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    @Test
    void previewStagesOnlyValidAndConfirmationPersistsOnce() throws Exception {
        long expedientCount = count("expedients"),
                graduateCount = count("graduates"),
                workCount = count("research_works");
        String[] observed = row("observed");
        observed[6] = "Escuela inexistente " + suffix;
        String[] rejected = row("rejected");
        rejected[2] = "";
        var result = preview(new String[][] {row("valid"), observed, rejected});
        assertEquals(3, result.path("totalRecords").asInt());
        for (String key : List.of("validRecords", "observedRecords", "rejectedRecords"))
            assertEquals(1, result.path(key).asInt());
        assertEquals(expedientCount, count("expedients"));
        assertEquals(graduateCount, count("graduates"));
        assertEquals(workCount, count("research_works"));
        long id = result.path("importId").asLong();
        mvc.perform(post("/api/v1/data-imports/{id}/confirm", id).with(auth(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.importedRecords").value(1));
        mvc.perform(get("/api/v1/data-imports/{id}", id).with(auth(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(post("/api/v1/data-imports/{id}/confirm", id).with(auth(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("La importación ya fue confirmada"));
        assertEquals(expedientCount + 1, count("expedients"));
        assertEquals(graduateCount + 1, count("graduates"));
        assertEquals(workCount + 1, count("research_works"));
    }

    @Test
    void invalidFileMissingColumnForeignAndMissingBatchKeepHttpStatuses() throws Exception {
        mvc.perform(
                        multipart("/api/v1/data-imports/preview")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "bad.xlsx",
                                                "application/octet-stream",
                                                new byte[] {1, 2}))
                                .with(auth(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mvc.perform(
                        multipart("/api/v1/data-imports/preview")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "historico.xlsx",
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                workbook(false, new String[][] {row("valid")})))
                                .with(auth(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta la columna obligatoria: nombres"));
        long id = preview(new String[][] {row("valid")}).path("importId").asLong();
        mvc.perform(post("/api/v1/data-imports/{id}/confirm", id).with(auth(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(post("/api/v1/data-imports/{id}/confirm", Long.MAX_VALUE).with(auth(owner)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/data-imports")).andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentConfirmationsReturn200And409WithOneAggregate() throws Exception {
        long before = count("expedients");
        long id = preview(new String[][] {row("concurrent")}).path("importId").asLong();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks =
                    java.util.stream.IntStream.range(0, 2)
                            .mapToObj(
                                    i ->
                                            executor.submit(
                                                    () -> {
                                                        ready.countDown();
                                                        assertTrue(
                                                                start.await(10, TimeUnit.SECONDS));
                                                        return mvc.perform(
                                                                        post(
                                                                                        "/api/v1/data-imports/{id}/confirm",
                                                                                        id)
                                                                                .with(auth(owner)))
                                                                .andReturn()
                                                                .getResponse()
                                                                .getStatus();
                                                    }))
                            .toList();
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var statuses =
                    List.of(
                                    tasks.get(0).get(30, TimeUnit.SECONDS),
                                    tasks.get(1).get(30, TimeUnit.SECONDS))
                            .stream()
                            .sorted()
                            .toList();
            assertEquals(List.of(200, 409), statuses);
        }
        assertEquals(before + 1, count("expedients"));
    }

    @Test
    void catalogChangedAfterPreviewReturns409WithoutPartialImport() throws Exception {
        String schoolName = "Escuela QA " + suffix;
        Long schoolId =
                jdbc.queryForObject(
                        "insert into schools(name) values (?) returning id",
                        Long.class,
                        schoolName);
        try {
            String[] withSchool = row("school");
            withSchool[6] = schoolName;
            long id = preview(new String[][] {row("first"), withSchool}).path("importId").asLong();
            jdbc.update(
                    "update schools set name = ? where id = ?",
                    schoolName + " modificada",
                    schoolId);
            long before = count("expedients"), graduatesBefore = count("graduates");
            mvc.perform(post("/api/v1/data-imports/{id}/confirm", id).with(auth(owner)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
            assertEquals(before, count("expedients"));
            assertEquals(graduatesBefore, count("graduates"));
        } finally {
            jdbc.update("delete from schools where id = ?", schoolId);
        }
    }

    @Test
    void duplicateAppearingAfterPreviewRollsBackEveryRow() throws Exception {
        var pending = preview(new String[][] {row("first"), row("duplicate")});
        var concurrent = preview(new String[][] {row("duplicate")});
        mvc.perform(
                        post(
                                        "/api/v1/data-imports/{id}/confirm",
                                        concurrent.path("importId").asLong())
                                .with(auth(owner)))
                .andExpect(status().isOk());
        long before = count("expedients"),
                graduatesBefore = count("graduates"),
                worksBefore = count("research_works");
        mvc.perform(
                        post("/api/v1/data-imports/{id}/confirm", pending.path("importId").asLong())
                                .with(auth(owner)))
                .andExpect(status().isConflict());
        assertEquals(before, count("expedients"));
        assertEquals(graduatesBefore, count("graduates"));
        assertEquals(worksBefore, count("research_works"));
        mvc.perform(
                        get("/api/v1/data-imports/{id}", pending.path("importId").asLong())
                                .with(auth(owner)))
                .andExpect(jsonPath("$.status").value("PREVIEW"));
    }

    @Test
    void administrativeAccessCreationDuplicateAndDeactivationAffectProtectedRequests()
            throws Exception {
        String body =
                """
                {"email":"adm-created-%s@unsa.edu.pe","fullName":"Administrativo sintético","googleSubject":null,"role":"ADMIN_GT","active":true}
                """
                        .formatted(suffix);
        var created =
                mvc.perform(
                                post("/api/v1/accesses")
                                        .with(auth(owner))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(
                        patch("/api/v1/accesses/{id}/active", id)
                                .with(auth(owner))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
        assertTrue(users.findById(id).orElseThrow().isActive());
        mvc.perform(
                        post("/api/v1/accesses")
                                .with(auth(owner))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isConflict());
        for (boolean active : List.of(false, false, true)) {
            mvc.perform(
                            patch("/api/v1/accesses/{id}/active", id)
                                    .with(auth(owner))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"active\":" + active + "}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(active));
            assertEquals(active, users.findById(id).orElseThrow().isActive());
            for (String route : List.of("/api/v1/auth/me", "/api/v1/dashboard")) {
                mvc.perform(
                                get(route)
                                        .with(
                                                jwt().jwt(
                                                                j ->
                                                                        j.subject("synthetic")
                                                                                .claim(
                                                                                        "email",
                                                                                        "adm-created-"
                                                                                                + suffix
                                                                                                + "@unsa.edu.pe"))))
                        .andExpect(status().is(active ? 200 : 403));
            }
        }
        other.setActive(false);
        users.save(other);
        mvc.perform(get("/api/v1/accesses").with(auth(other))).andExpect(status().isForbidden());
    }
}

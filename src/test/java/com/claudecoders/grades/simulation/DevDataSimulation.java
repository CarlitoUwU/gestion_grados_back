package com.claudecoders.grades.simulation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.claudecoders.grades.expedient.ExpedientRepository;
import com.claudecoders.grades.expedientstatus.ExpedientStatusRepository;
import com.claudecoders.grades.modality.DegreeModalityRepository;
import com.claudecoders.grades.school.SchoolRepository;
import com.claudecoders.grades.teacher.TeacherRepository;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Genera datos de desarrollo recorriendo la API real (sin rollback): alta de expedientes,
 * resoluciones con PDF, asesor, jurado, sorteos, sustentación, estados, carga histórica y accesos.
 * También comprueba los rechazos de versión, PDF y autodesactivación.
 *
 * <p>Solo se ejecuta con SIMULATION=true. Requiere los catálogos de scripts/seed_catalogos_dev.sql,
 * el correo de un ADMIN_GT existente en SIMULATION_EMAIL y PDF en SIMULATION_PDF_DIR.
 */
@SpringBootTest(
        properties = {
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/keys",
            "logging.level.org.hibernate.orm.jdbc.bind=OFF",
            "logging.level.org.hibernate.SQL=OFF"
        })
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "SIMULATION", matches = "true")
class DevDataSimulation {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final String[] FIRST =
            "Lucía,Mateo,Valeria,Diego,Camila,Andrés,Sofía,Gabriel,Daniela,Nicolás,Elena,Joaquín,Ximena,Renzo,Fiorella,Sebastián,Milagros,Álvaro,Alessandra,Rodrigo,Jimena,Bruno,Kiara,Fabricio"
                    .split(",");
    private static final String[] LAST =
            "Paredes Quispe,Salazar Mamani,Núñez Ccama,Rojas Apaza,Torres Huamán,Medina Condori,Herrera Pari,Vera Chambi,Castro Ticona,Flores Coaguila,Ruiz Valdivia,Paz Zegarra,Gutiérrez Llerena,Cáceres Begazo,Málaga Tejada,Pinto Arce,Velarde Cornejo,Bustamante Rivera,Linares Yucra,Salas Benavides,Ochoa Chávez,Delgado Neyra,Talavera Soto,Zúñiga Puma"
                    .split(",");
    private static final Map<String, String[]> TITLES =
            Map.of(
                    "Ingeniería de Sistemas",
                    new String[] {
                        "Modelo predictivo de deserción estudiantil con aprendizaje automático",
                        "Arquitectura de microservicios para la gestión de trámites académicos",
                        "Detección de intrusiones en redes universitarias mediante redes neuronales",
                        "Sistema de recomendación de cursos basado en historial académico"
                    },
                    "Ingeniería Civil",
                    new String[] {
                        "Evaluación sísmica de edificaciones de albañilería en Cayma",
                        "Comportamiento de concretos con agregado reciclado en Arequipa",
                        "Diseño de pavimentos flexibles para vías de alta montaña"
                    },
                    "Administración",
                    new String[] {
                        "Gestión del talento humano y productividad en mypes de Arequipa",
                        "Transformación digital en empresas exportadoras del sur del Perú",
                        "Calidad de servicio y satisfacción en entidades financieras"
                    },
                    "Economía",
                    new String[] {
                        "Impacto del canon minero en el desarrollo regional de Arequipa",
                        "Informalidad laboral y productividad en el Perú 2010-2024",
                        "Determinantes de la inclusión financiera rural"
                    },
                    "Educación",
                    new String[] {
                        "Estrategias de comprensión lectora en educación secundaria",
                        "Uso de entornos virtuales en la enseñanza de la matemática",
                        "Convivencia escolar y rendimiento académico en Arequipa"
                    },
                    "Contabilidad",
                    new String[] {
                        "Control interno y gestión de inventarios en empresas comerciales",
                        "Aplicación de NIIF 16 en empresas del sector retail",
                        "Cultura tributaria en contribuyentes del régimen MYPE"
                    });
    private static final Map<String, String> FACULTY =
            Map.of(
                    "Ingeniería de Sistemas", "FIPS",
                    "Ingeniería Civil", "FIC",
                    "Administración", "FCA",
                    "Economía", "FE",
                    "Educación", "FCE",
                    "Contabilidad", "FCCF");

    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;
    @Autowired SchoolRepository schools;
    @Autowired DegreeModalityRepository modalities;
    @Autowired ExpedientStatusRepository statuses;
    @Autowired TeacherRepository teachers;
    @Autowired ExpedientRepository expedients;

    private final Random random = new Random(2026);
    private final List<byte[]> pdfs = new ArrayList<>();
    private final Map<String, Integer> totals = new LinkedHashMap<>();
    private int pdfIndex;
    private int resolutionSeq = 100;

    RequestPostProcessor auth() {
        String email = System.getenv().getOrDefault("SIMULATION_EMAIL", "");
        var token = jwt().jwt(j -> j.subject("simulacion-local").claim("email", email));
        // Los fileUrl generados deben apuntar al puerto real de la API de desarrollo.
        return request -> {
            request.setServerPort(8080);
            return token.postProcessRequest(request);
        };
    }

    JsonNode json(ResultActions result) throws Exception {
        return mapper.readTree(
                result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    ResultActions send(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return mvc.perform(
                request.with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)));
    }

    @Test
    void simulateFullFlow() throws Exception {
        loadPdfs();
        assertFalse(
                expedients.findAll().stream().anyMatch(e -> e.getNumber().startsWith("GT-")),
                "La simulación ya se ejecutó en esta base");
        Map<String, Long> school = new LinkedHashMap<>();
        schools.findAll().forEach(s -> school.put(s.getName(), s.getId()));
        Map<String, Long> modality = new LinkedHashMap<>();
        modalities.findAll().forEach(m -> modality.put(m.getName(), m.getId()));
        Map<String, Long> status = new LinkedHashMap<>();
        statuses.findAll().forEach(s -> status.put(s.getName(), s.getId()));
        List<Long> teacherIds = teachers.findAll().stream().map(t -> t.getId()).sorted().toList();
        assertTrue(
                school.size() >= 6
                        && modality.size() >= 2
                        && status.size() >= 5
                        && teacherIds.size() >= 8,
                "Faltan catálogos: ejecuta scripts/seed_catalogos_dev.sql");

        LocalDate today = LocalDate.now(LIMA);
        List<String> schoolNames = new ArrayList<>(TITLES.keySet());
        schoolNames.sort(String::compareTo);
        int[] stages = {0, 1, 1, 2, 3, 3, 3, 4, 4, 4, 5, 5};
        long concurrencyId = 0;
        for (int i = 0; i < 24; i++) {
            int stage = stages[i % stages.length];
            String schoolName = schoolNames.get(i % schoolNames.size());
            String modalityName = i % 3 == 2 ? "Artículo de investigación" : "Tesis";
            LocalDate start = today.minusDays(60L + stage * 130L + random.nextInt(180));
            String number = "GT-%d-%04d".formatted(start.getYear(), i + 1);
            String title = TITLES.get(schoolName)[i % TITLES.get(schoolName).length];
            var created =
                    json(
                            send(
                                            post("/api/v1/expedients/complete"),
                                            Map.of(
                                                    "number", number,
                                                    "startDate", start.toString(),
                                                    "modalityId", modality.get(modalityName),
                                                    "statusId", status.get("Registrado"),
                                                    "graduate",
                                                            Map.of(
                                                                    "documentNumber",
                                                                    "7%07d"
                                                                            .formatted(
                                                                                    1200000
                                                                                            + i
                                                                                                    * 7919),
                                                                    "firstNames",
                                                                    FIRST[i],
                                                                    "lastNames",
                                                                    LAST[i],
                                                                    "schoolId",
                                                                    school.get(schoolName),
                                                                    "academicProgram",
                                                                    schoolName),
                                                    "research",
                                                            Map.of(
                                                                    "title", title,
                                                                    "workType", modalityName)))
                                    .andExpect(status().isCreated()));
            long id = created.path("summary").path("id").asLong();
            count("expedientes");
            if (stage == 0) continue;

            String faculty = FACULTY.get(schoolName);
            int t = i % teacherIds.size();
            Long advisor = teacherIds.get(t);
            List<Map<String, Object>> resolutions = new ArrayList<>();
            resolutions.add(resolution("ra", faculty, start.plusDays(20), "Designación de asesor"));
            Map<String, Object> academic = new LinkedHashMap<>();
            academic.put("resolutions", resolutions);
            academic.put(
                    "advisor",
                    Map.of(
                            "teacherId",
                            advisor,
                            "resolutionClientId",
                            "ra",
                            "assignedAt",
                            start.plusDays(20).toString()));
            academic.put("jury", null);
            academic.put("draws", List.of());
            academic.put("defense", null);
            if (stage >= 3) {
                resolutions.add(
                        resolution("rj", faculty, start.plusDays(120), "Designación de jurado"));
                String[] roles = {"PRESIDENTE", "SECRETARIO", "VOCAL", "ACCESITARIO"};
                List<Map<String, Object>> jury = new ArrayList<>();
                List<Long> drawn = new ArrayList<>();
                for (int r = 0; r < roles.length; r++) {
                    Long teacher = teacherIds.get((t + 1 + r) % teacherIds.size());
                    drawn.add(teacher);
                    jury.add(
                            Map.of(
                                    "teacherId",
                                    teacher,
                                    "role",
                                    roles[r],
                                    "resolutionClientId",
                                    "rj"));
                }
                academic.put("jury", jury);
                academic.put(
                        "draws",
                        List.of(
                                Map.of(
                                        "date",
                                        start.plusDays(110).toString(),
                                        "resolutionClientId",
                                        "rj",
                                        "teacherIds",
                                        drawn)));
                boolean defended = stage >= 4;
                LocalDate defense =
                        defended ? start.plusDays(300) : today.plusDays(7 + random.nextInt(30));
                if (defended)
                    resolutions.add(
                            resolution(
                                    "rs", faculty, defense.minusDays(10), "Fecha de sustentación"));
                academic.put(
                        "defense",
                        Map.of(
                                "date",
                                defense.toString(),
                                "result",
                                !defended ? "Pendiente" : i == 8 ? "Desaprobado" : "Aprobado"));
            }
            JsonNode detail = putAcademic(id, academic);
            count("asignaciones académicas");
            for (JsonNode res : detail.path("resolutions")) uploadPdf(res.path("id").asLong());

            String finalStatus =
                    switch (stage) {
                        case 2 -> "Observado";
                        case 4 -> i == 8 ? "Observado" : "Sustentado";
                        case 5 -> "Completado";
                        default -> "En trámite";
                    };
            send(
                            patch("/api/v1/expedients/{id}/status", id),
                            Map.of("statusId", status.get(finalStatus)))
                    .andExpect(status().isOk());
            count("cambios de estado");

            if (stage == 5) {
                var extra =
                        json(
                                send(
                                                post("/api/v1/resolutions"),
                                                resolutionRequest(
                                                        id,
                                                        faculty,
                                                        start.plusDays(340),
                                                        "Otorgamiento de grado"))
                                        .andExpect(status().isCreated()));
                send(
                                put("/api/v1/resolutions/{id}", extra.path("id").asLong()),
                                Map.of(
                                        "expedientId",
                                        id,
                                        "number",
                                        extra.path("number").asString(),
                                        "resolutionDate",
                                        start.plusDays(340).toString(),
                                        "resolutionType",
                                        "Otorgamiento de grado académico",
                                        "fileUrl",
                                        ""))
                        .andExpect(status().isOk());
                // El PDF se sube después de editar para que la edición no limpie su enlace.
                uploadPdf(extra.path("id").asLong());
                count("resoluciones");
            }
            if (stage == 3 && concurrencyId == 0) concurrencyId = id;
        }

        checkRejections(concurrencyId);
        historicalImport(today);
        accesses();
        verifyReads();
        System.out.println("SIMULACION " + totals);
    }

    Map<String, Object> resolution(String clientId, String faculty, LocalDate date, String type) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("clientId", clientId);
        value.put("number", "R-%04d-%d-%s".formatted(++resolutionSeq, date.getYear(), faculty));
        value.put("date", date.toString());
        value.put("type", type);
        count("resoluciones");
        return value;
    }

    Map<String, Object> resolutionRequest(
            long expedientId, String faculty, LocalDate date, String type) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("expedientId", expedientId);
        value.put("number", "R-%04d-%d-%s".formatted(++resolutionSeq, date.getYear(), faculty));
        value.put("resolutionDate", date.toString());
        value.put("resolutionType", type);
        value.put("fileUrl", null);
        return value;
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

    JsonNode putAcademic(long id, Object body) throws Exception {
        return json(
                mvc.perform(
                                put("/api/v1/expedients/{id}/academic", id)
                                        .with(auth())
                                        .header("If-Match", etag(id))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(mapper.writeValueAsString(body)))
                        .andExpect(status().isOk()));
    }

    void uploadPdf(long resolutionId) throws Exception {
        byte[] pdf = pdfs.get(pdfIndex++ % pdfs.size());
        mvc.perform(
                        multipart("/api/v1/resolutions/{id}/document", resolutionId)
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "resolucion-" + resolutionId + ".pdf",
                                                "application/pdf",
                                                pdf))
                                .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileUrl").exists());
        mvc.perform(get("/api/v1/resolutions/{id}/document", resolutionId).with(auth()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdf));
        count("PDF almacenados");
    }

    void checkRejections(long id) throws Exception {
        String stale = etag(id);
        JsonNode detail =
                json(
                        mvc.perform(get("/api/v1/expedients/{id}/detail", id).with(auth()))
                                .andExpect(status().isOk()));
        Map<String, Object> body = new LinkedHashMap<>();
        List<Map<String, Object>> keep = new ArrayList<>();
        for (JsonNode r : detail.path("resolutions")) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.path("id").asLong());
            item.put("clientId", "c" + r.path("id").asLong());
            item.put("number", r.path("number").asString());
            item.put("date", r.path("date").isNull() ? null : r.path("date").asString());
            item.put("type", "Designación (revisada)");
            keep.add(item);
        }
        body.put("resolutions", keep);
        var advisor = detail.path("advisor");
        Map<String, Object> advisorBody = new LinkedHashMap<>();
        advisorBody.put("id", advisor.path("id").asLong());
        advisorBody.put("teacherId", advisor.path("teacherId").asLong());
        advisorBody.put("resolutionClientId", "c" + advisor.path("resolutionId").asLong());
        advisorBody.put("assignedAt", advisor.path("assignedAt").asString());
        body.put("advisor", advisorBody);
        List<Map<String, Object>> jury = new ArrayList<>();
        for (JsonNode j : detail.path("jury")) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", j.path("id").asLong());
            m.put("teacherId", j.path("teacherId").asLong());
            m.put("role", j.path("role").asString());
            m.put("resolutionClientId", "c" + j.path("resolutionId").asLong());
            jury.add(m);
        }
        body.put("jury", jury);
        List<Map<String, Object>> draws = new ArrayList<>();
        for (JsonNode d : detail.path("draws")) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.path("id").asLong());
            m.put("date", d.path("date").asString());
            m.put("resolutionClientId", "c" + d.path("resolutionId").asLong());
            List<Long> ids = new ArrayList<>();
            d.path("teachers").forEach(x -> ids.add(x.path("teacherId").asLong()));
            m.put("teacherIds", ids);
            draws.add(m);
        }
        body.put("draws", draws);
        var defense = detail.path("defense");
        body.put(
                "defense",
                Map.of(
                        "date",
                        defense.path("date").asString(),
                        "result",
                        defense.path("result").asString()));

        mvc.perform(
                        put("/api/v1/expedients/{id}/academic", id)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(body)))
                .andExpect(status().isPreconditionRequired());
        putAcademic(id, body);
        mvc.perform(
                        put("/api/v1/expedients/{id}/academic", id)
                                .with(auth())
                                .header("If-Match", stale)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(body)))
                .andExpect(status().isPreconditionFailed());
        var fileUrls = json(mvc.perform(get("/api/v1/expedients/{id}/detail", id).with(auth())));
        fileUrls.path("resolutions")
                .forEach(
                        r ->
                                assertFalse(
                                        r.path("fileUrl").isNull(),
                                        "El reemplazo académico perdió un PDF"));
        count("rechazos verificados (428/412)");

        long resolutionId = detail.path("resolutions").get(0).path("id").asLong();
        byte[] big = new byte[11 * 1024 * 1024];
        System.arraycopy("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII), 0, big, 0, 9);
        mvc.perform(
                        multipart("/api/v1/resolutions/{id}/document", resolutionId)
                                .file(
                                        new MockMultipartFile(
                                                "file", "grande.pdf", "application/pdf", big))
                                .with(auth()))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(
                        multipart("/api/v1/resolutions/{id}/document", resolutionId)
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "nota.txt",
                                                "text/plain",
                                                "no es pdf".getBytes(StandardCharsets.UTF_8)))
                                .with(auth()))
                .andExpect(status().isBadRequest());
        count("rechazos verificados (413/400 PDF)");
    }

    void historicalImport(LocalDate today) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Histórico");
            String[][] rows = {
                {
                    "Número de expediente",
                    "Fecha de inicio",
                    "Documento",
                    "Nombres",
                    "Apellidos",
                    "Escuela",
                    "Programa académico",
                    "Modalidad",
                    "Estado",
                    "Título del trabajo"
                },
                {
                    "HIST-2021-0001",
                    "2021-03-15",
                    "45120987",
                    "Martín",
                    "Arenas Valencia",
                    "Ingeniería de Sistemas",
                    "Ingeniería de Sistemas",
                    "Tesis",
                    "Completado",
                    "Sistema de información para bibliotecas municipales"
                },
                {
                    "HIST-2021-0002",
                    "2021-06-02",
                    "45230198",
                    "Paola",
                    "Bernal Quispe",
                    "Economía",
                    "Economía",
                    "Tesis",
                    "Completado",
                    "Crecimiento económico y pobreza en Arequipa 2005-2019"
                },
                {
                    "HIST-2022-0001",
                    "2022-04-11",
                    "46340219",
                    "Iván",
                    "Coronado Pari",
                    "Ingeniería Civil",
                    "Ingeniería Civil",
                    "Artículo de investigación",
                    "Completado",
                    "Resistencia de adobes estabilizados con cal"
                },
                {
                    "HIST-2022-0002",
                    "2022-09-20",
                    "46450321",
                    "Lorena",
                    "Díaz Mamani",
                    "Administración",
                    "Administración",
                    "Tesis",
                    "Completado",
                    "Clima organizacional en municipalidades distritales"
                },
                {
                    "HIST-2022-0003",
                    "2022-11-05",
                    "46560432",
                    "Raúl",
                    "Espinoza Ccori",
                    "Educación",
                    "Educación",
                    "Tesis",
                    "Completado",
                    "Aprendizaje cooperativo en educación primaria"
                },
                {
                    "HIST-2022-0004",
                    "2022-12-01",
                    "46670543",
                    "Gisela",
                    "Farfán Huamaní",
                    "Escuela de Prueba",
                    "Desconocido",
                    "Tesis",
                    "Completado",
                    "Registro con escuela fuera del catálogo"
                },
                {
                    "HIST-2022-0005",
                    "2022-12-10",
                    "46780654",
                    "",
                    "Gamarra Soto",
                    "Contabilidad",
                    "Contabilidad",
                    "Tesis",
                    "Completado",
                    "Registro sin nombres"
                },
            };
            for (int r = 0; r < rows.length; r++) {
                var row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) row.createCell(c).setCellValue(rows[r][c]);
            }
            workbook.write(out);
        }
        var preview =
                json(
                        mvc.perform(
                                        multipart("/api/v1/data-imports/preview")
                                                .file(
                                                        new MockMultipartFile(
                                                                "file",
                                                                "historico-grados.xlsx",
                                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                                out.toByteArray()))
                                                .with(auth()))
                                .andExpect(status().isOk()));
        long importId = preview.path("importId").asLong();
        assertTrue(importId > 0, preview.toString());
        mvc.perform(post("/api/v1/data-imports/{id}/confirm", importId).with(auth()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/data-imports/{id}/confirm", importId).with(auth()))
                .andExpect(status().isConflict());
        assertTrue(expedients.existsByNumberIgnoreCase("HIST-2021-0001"));
        assertFalse(expedients.existsByNumberIgnoreCase("HIST-2022-0005"));
        totals.put(
                "carga histórica (filas válidas importadas)", preview.path("validRecords").asInt());
        System.out.println("SIMULACION preview " + preview);
    }

    void accesses() throws Exception {
        var me = json(mvc.perform(get("/api/v1/auth/me").with(auth())).andExpect(status().isOk()));
        send(patch("/api/v1/accesses/{id}/active", me.path("id").asLong()), Map.of("active", false))
                .andExpect(status().isConflict());
        for (String[] access :
                new String[][] {
                    {
                        "simulacion.secretaria@unsa.edu.pe",
                        "Secretaría de Grados (simulación)",
                        "true"
                    },
                    {
                        "simulacion.exadmin@unsa.edu.pe",
                        "Administrativo cesado (simulación)",
                        "false"
                    }
                }) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("email", access[0]);
            body.put("fullName", access[1]);
            body.put("googleSubject", null);
            body.put("role", "ADMIN_GT");
            body.put("active", true);
            long id =
                    json(send(post("/api/v1/accesses"), body).andExpect(status().isCreated()))
                            .path("id")
                            .asLong();
            if (access[2].equals("false"))
                send(patch("/api/v1/accesses/{id}/active", id), Map.of("active", false))
                        .andExpect(status().isOk());
            count("accesos");
        }
    }

    void verifyReads() throws Exception {
        var dashboard =
                json(mvc.perform(get("/api/v1/dashboard").with(auth())).andExpect(status().isOk()));
        assertTrue(dashboard.path("total").asInt() >= 24);
        System.out.println(
                "SIMULACION dashboard "
                        + dashboard.path("total")
                        + " total, "
                        + dashboard.path("defended")
                        + " sustentados, "
                        + dashboard.path("observed")
                        + " observados");
        for (String report : List.of("statistics", "teachers", "draws", "defended-works")) {
            var rows =
                    json(mvc.perform(get("/api/v1/reports/" + report).with(auth()))
                                    .andExpect(status().isOk()))
                            .path("rows");
            assertTrue(rows.size() > 0, "Reporte vacío: " + report);
            System.out.println("SIMULACION reporte " + report + ": " + rows.size() + " filas");
        }
    }

    void loadPdfs() throws Exception {
        Path dir =
                Path.of(
                        System.getenv()
                                .getOrDefault("SIMULATION_PDF_DIR", "build/simulation-pdfs"));
        try (var files = Files.list(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".pdf")).sorted().toList()) {
                byte[] bytes = Files.readAllBytes(file);
                if (bytes.length <= 10 * 1024 * 1024
                        && Arrays.equals(
                                Arrays.copyOf(bytes, 5),
                                "%PDF-".getBytes(StandardCharsets.US_ASCII))) pdfs.add(bytes);
            }
        }
        assertFalse(pdfs.isEmpty(), "No hay PDF en " + dir);
    }

    void count(String key) {
        totals.merge(key, 1, Integer::sum);
    }
}

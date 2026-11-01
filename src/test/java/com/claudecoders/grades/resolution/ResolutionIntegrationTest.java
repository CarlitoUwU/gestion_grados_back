package com.claudecoders.grades.resolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claudecoders.grades.expedient.Expedient;
import com.claudecoders.grades.expedient.ExpedientRepository;
import com.claudecoders.grades.expedientadvisor.ExpedientAdvisor;
import com.claudecoders.grades.expedientadvisor.ExpedientAdvisorRepository;
import com.claudecoders.grades.graduate.Graduate;
import com.claudecoders.grades.graduate.GraduateRepository;
import com.claudecoders.grades.teacher.Teacher;
import com.claudecoders.grades.teacher.TeacherRepository;
import com.claudecoders.grades.user.User;
import com.claudecoders.grades.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/keys")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class ResolutionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;
    @Autowired UserRepository users;
    @Autowired GraduateRepository graduates;
    @Autowired ExpedientRepository expedients;
    @Autowired ResolutionRepository resolutions;
    @Autowired ExpedientAdvisorRepository advisors;
    @Autowired TeacherRepository teachers;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    private User user;
    private Expedient first;
    private Expedient second;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        user = new User();
        user.setEmail("resolutions-" + suffix + "@unsa.edu.pe");
        user.setGoogleSubject("resolutions-subject-" + suffix);
        user.setFullName("Usuario resoluciones " + suffix);
        user.setRole("ADMIN_GT");
        user = users.saveAndFlush(user);
        first = expedient("RES-EXP-" + suffix);
        second = expedient("RES-EXP-2-" + suffix);
    }

    @Test
    void createUpdateAndDeleteResolutionUpdateExpedientMetadata() throws Exception {
        resetUpdatedAt(first);
        String number = "RES-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult created =
                mvc.perform(
                                post("/api/v1/resolutions")
                                        .with(jwtFor(user))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"expedientId":%d,"number":"%s",
                                                 "resolutionDate":"2026-10-01",
                                                 "resolutionType":"Asesor",
                                                 "fileUrl":"https://documentos.example/res.pdf"}
                                                """
                                                        .formatted(first.getId(), number)))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andExpect(jsonPath("$.id").isNumber())
                        .andExpect(jsonPath("$.expedientId").value(first.getId()))
                        .andExpect(jsonPath("$.number").value(number))
                        .andReturn();
        JsonNode body = mapper.readTree(created.getResponse().getContentAsString());
        long id = body.path("id").asLong();
        assertTouched(first.getId());

        mvc.perform(get("/api/v1/expedients/{id}/detail", first.getId()).with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.updatedByName").value(user.getFullName()))
                .andExpect(jsonPath("$.resolutions[0].id").value(id));
        mvc.perform(get("/api/v1/resolutions").with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
        mvc.perform(get("/api/v1/resolutions/{id}", id).with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(number));

        String updatedNumber = number + "-B";
        resetUpdatedAt(first);
        mvc.perform(
                        put("/api/v1/resolutions/{id}", id)
                                .with(jwtFor(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"expedientId":%d,"number":"%s",
                                         "resolutionDate":null,"resolutionType":null,"fileUrl":null}
                                        """
                                                .formatted(first.getId(), updatedNumber)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(updatedNumber))
                .andExpect(jsonPath("$.resolutionDate").isEmpty())
                .andExpect(jsonPath("$.resolutionType").isEmpty())
                .andExpect(jsonPath("$.fileUrl").isEmpty());
        assertTouched(first.getId());

        mvc.perform(
                        post("/api/v1/resolutions")
                                .with(jwtFor(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"expedientId\":"
                                                + first.getId()
                                                + ",\"number\":\""
                                                + updatedNumber.toLowerCase()
                                                + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mvc.perform(
                        put("/api/v1/resolutions/{id}", id)
                                .with(jwtFor(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"expedientId\":"
                                                + first.getId()
                                                + ",\"number\":\""
                                                + updatedNumber
                                                + "\"}"))
                .andExpect(status().isOk());

        resetUpdatedAt(first);
        mvc.perform(delete("/api/v1/resolutions/{id}", id).with(jwtFor(user)))
                .andExpect(status().isNoContent());
        assertTouched(first.getId());
        mvc.perform(get("/api/v1/resolutions/{id}", id).with(jwtFor(user)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/resolutions/{id}", id).with(jwtFor(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReferencedResolutionReturns409EvenIfClientDidNotCheck() throws Exception {
        Resolution resolution = createResolution(first, "USED-" + UUID.randomUUID());
        Teacher teacher = teacher();
        ExpedientAdvisor advisor = new ExpedientAdvisor();
        advisor.setExpedient(first);
        advisor.setTeacher(teacher);
        advisor.setResolution(resolution);
        advisor.setAssignedAt(LocalDate.of(2026, 10, 1));
        advisors.saveAndFlush(advisor);

        mvc.perform(delete("/api/v1/resolutions/{id}", resolution.getId()).with(jwtFor(user)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mvc.perform(get("/api/v1/resolutions/{id}", resolution.getId()).with(jwtFor(user)))
                .andExpect(status().isOk());
    }

    @Test
    void movingReferencedResolutionToAnotherExpedientReturns409() throws Exception {
        Resolution resolution = createResolution(first, "MOVE-" + UUID.randomUUID());
        Teacher teacher = teacher();
        ExpedientAdvisor advisor = new ExpedientAdvisor();
        advisor.setExpedient(first);
        advisor.setTeacher(teacher);
        advisor.setResolution(resolution);
        advisors.saveAndFlush(advisor);

        mvc.perform(
                        put("/api/v1/resolutions/{id}", resolution.getId())
                                .with(jwtFor(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"expedientId\":"
                                                + second.getId()
                                                + ",\"number\":\""
                                                + resolution.getNumber()
                                                + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/expedients/{id}/detail", first.getId()).with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolutions[0].id").value(resolution.getId()));
    }

    private Resolution createResolution(Expedient expedient, String number) throws Exception {
        MvcResult result =
                mvc.perform(
                                post("/api/v1/resolutions")
                                        .with(jwtFor(user))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"expedientId\":"
                                                        + expedient.getId()
                                                        + ",\"number\":\""
                                                        + number
                                                        + "\"}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        long id = mapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
        return resolutions.findById(id).orElseThrow();
    }

    private void assertTouched(Long expedientId) {
        entityManager.clear();
        Expedient refreshed = expedients.findById(expedientId).orElseThrow();
        assertEquals(user.getId(), refreshed.getUpdatedBy().getId());
        assertEquals(user.getFullName(), refreshed.getUpdatedBy().getFullName());
        assertTrue(refreshed.getUpdatedAt().isAfter(LocalDateTime.of(2020, 1, 1, 0, 0)));
    }

    private void resetUpdatedAt(Expedient expedient) {
        jdbc.update(
                "update expedients set updated_at = ? where id = ?",
                Timestamp.valueOf(LocalDateTime.of(2020, 1, 1, 0, 0)),
                expedient.getId());
        entityManager.clear();
    }

    private Expedient expedient(String number) {
        Graduate graduate = new Graduate();
        graduate.setFirstNames("Nombre " + number);
        graduate.setLastNames("Apellido");
        graduate = graduates.saveAndFlush(graduate);
        Expedient expedient = new Expedient();
        expedient.setNumber(number);
        expedient.setStartDate(LocalDate.of(2026, 1, 1));
        expedient.setGraduate(graduate);
        return expedients.saveAndFlush(expedient);
    }

    private Teacher teacher() {
        Teacher teacher = new Teacher();
        teacher.setFullName("Docente " + UUID.randomUUID());
        return teachers.saveAndFlush(teacher);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(
            User user) {
        return jwt().jwt(token -> token.subject(user.getGoogleSubject()));
    }
}

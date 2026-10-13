package com.claudecoders.grades.expedient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claudecoders.grades.dataimport.dto.StagedGraduateRecord;
import com.claudecoders.grades.expedientstatus.ExpedientStatus;
import com.claudecoders.grades.expedientstatus.ExpedientStatusRepository;
import com.claudecoders.grades.modality.DegreeModality;
import com.claudecoders.grades.modality.DegreeModalityRepository;
import com.claudecoders.grades.resolution.ResolutionRepository;
import com.claudecoders.grades.school.School;
import com.claudecoders.grades.school.SchoolRepository;
import com.claudecoders.grades.teacher.Teacher;
import com.claudecoders.grades.teacher.TeacherRepository;
import com.claudecoders.grades.user.User;
import com.claudecoders.grades.user.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/keys")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ExpedientIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;
    @Autowired UserRepository users;
    @Autowired SchoolRepository schools;
    @Autowired DegreeModalityRepository modalities;
    @Autowired ExpedientStatusRepository statuses;
    @Autowired TeacherRepository teachers;
    @Autowired ResolutionRepository resolutions;

    @Test
    void importPreviewDatesRoundTripWithConfiguredJsonMapper() {
        var staged =
                new StagedGraduateRecord(
                        1,
                        "A-1",
                        LocalDate.of(2025, 1, 2),
                        "123",
                        "Nombre",
                        "Apellido",
                        null,
                        null,
                        null,
                        null,
                        "Investigación");
        String json = mapper.writeValueAsString(java.util.List.of(staged));
        var restored =
                mapper.readValue(
                        json, new TypeReference<java.util.List<StagedGraduateRecord>>() {});
        assertEquals(staged, restored.getFirst());
    }

    @Test
    void completeSearchStatusAndAcademicReplacement() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setEmail(suffix + "@test.local");
        user.setGoogleSubject("user-" + suffix);
        String subject = user.getGoogleSubject();
        user.setFullName("Operador " + suffix);
        user = users.save(user);
        School school = new School();
        school.setName("Escuela " + suffix);
        school = schools.save(school);
        DegreeModality modality = new DegreeModality();
        modality.setName("Modalidad " + suffix);
        modality = modalities.save(modality);
        ExpedientStatus statusOne = new ExpedientStatus();
        statusOne.setName("Inicio " + suffix);
        statusOne = statuses.save(statusOne);
        ExpedientStatus statusTwo = new ExpedientStatus();
        statusTwo.setName("Revisión " + suffix);
        statusTwo = statuses.save(statusTwo);
        Teacher teacher = new Teacher();
        teacher.setFullName("Docente " + suffix);
        teacher = teachers.save(teacher);
        String number = "EXP-" + suffix.toUpperCase();
        String complete =
                """
                {"number":"%s","startDate":"2025-01-01","modalityId":%d,"statusId":%d,
                "graduate":{"documentNumber":"DNI-%s","fullName":"Nombre Completo","schoolId":%d},
                "research":{"title":"Título %s","workType":"TESIS"}}
                """
                        .formatted(
                                number.toLowerCase(),
                                modality.getId(),
                                statusOne.getId(),
                                suffix,
                                school.getId(),
                                suffix);
        mvc.perform(
                        post("/api/v1/expedients/complete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(complete))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(
                        post("/api/v1/expedients/complete")
                                .header("Authorization", "Bearer invalid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(complete))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(
                        post("/api/v1/expedients/complete")
                                .with(jwt().jwt(j -> j.subject("other")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(complete))
                .andExpect(status().isForbidden());

        var created =
                mvc.perform(
                                post("/api/v1/expedients/complete")
                                        .with(jwt().jwt(j -> j.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(complete))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.summary.number").value(number))
                        .andExpect(jsonPath("$.summary.graduateLastNames").value(""))
                        .andExpect(jsonPath("$.summary.createdByName").value(user.getFullName()))
                        .andReturn();
        JsonNode detail = mapper.readTree(created.getResponse().getContentAsString());
        long id = detail.path("summary").path("id").asLong();
        long graduateId = detail.path("summary").path("graduateId").asLong();
        mvc.perform(get("/api/v1/expedients/search").param("search", "DNI-" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/v1/expedients/search").param("search", "Título " + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(
                        patch("/api/v1/expedients/{id}/status", id)
                                .with(jwt().jwt(j -> j.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"statusId\":" + statusTwo.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusId").value(statusTwo.getId()));

        String academic =
                """
                {"resolutions":[{"clientId":"r1","number":"R-1","date":"2025-02-01","type":"NOMBRAMIENTO"}],
                "advisor":{"teacherId":%d,"resolutionClientId":"r1"},"jury":null,
                "draws":[{"date":"2025-02-02","resolutionClientId":"r1","teacherIds":[%d]}],"defense":null}
                """
                        .formatted(teacher.getId(), teacher.getId());
        mvc.perform(
                        put("/api/v1/expedients/{id}/academic", id)
                                .with(jwt().jwt(j -> j.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(academic))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolutions[0].number").value("R-1"))
                .andExpect(jsonPath("$.draws[0].teachers[0].teacherId").value(teacher.getId()));
        var resolution =
                resolutions.findByExpedientIdOrderByResolutionDateDescIdDesc(id).getFirst();
        resolution.setFileUrl("https://example.test/document.pdf");
        resolutions.saveAndFlush(resolution);
        String retained =
                academic.replace(
                        "\"clientId\":\"r1\"",
                        "\"id\":" + resolution.getId() + ",\"clientId\":\"r1\"");
        mvc.perform(
                        put("/api/v1/expedients/{id}/academic", id)
                                .with(jwt().jwt(j -> j.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(retained))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolutions[0].id").value(resolution.getId()))
                .andExpect(
                        jsonPath("$.resolutions[0].fileUrl")
                                .value("https://example.test/document.pdf"));
        String invalid = academic.replace("\"r1\",\"teacherIds\"", "\"r3\",\"teacherIds\"");
        mvc.perform(
                        put("/api/v1/expedients/{id}/academic", id)
                                .with(jwt().jwt(j -> j.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors['draws[0].resolutionClientId']").exists());
        mvc.perform(get("/api/v1/expedients/{id}/detail", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolutions[0].number").value("R-1"));
        mvc.perform(
                        post("/api/v1/expedients")
                                .with(jwt().jwt(j -> j.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"number\":\"SECOND-"
                                                + suffix
                                                + "\",\"startDate\":\"2025-01-01\",\"graduateId\":"
                                                + graduateId
                                                + ",\"createdById\":999999,\"updatedById\":999999}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdById").value(user.getId()));
        mvc.perform(
                        put("/api/v1/expedients/{id}/complete", id)
                                .with(jwt().jwt(j -> j.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(complete.replace("Nombre Completo", "Nombre Nuevo")))
                .andExpect(status().isConflict());
        mvc.perform(
                        put("/api/v1/expedients/{id}/academic", id)
                                .with(jwt().jwt(j -> j.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"resolutions\":[],\"jury\":null,\"draws\":[],\"advisor\":null,\"defense\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolutions.length()").value(0));
        mvc.perform(get("/api/v1/catalogs/schools").with(jwt().jwt(j -> j.subject(subject))))
                .andExpect(status().isOk());
    }
}

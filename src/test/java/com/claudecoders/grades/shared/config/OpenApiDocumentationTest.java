package com.claudecoders.grades.shared.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        properties =
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/keys")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class OpenApiDocumentationTest {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;

    @Test
    void openApiDocumentsSecurityDashboardErrorsAndResolutionContract() throws Exception {
        String content =
                mvc.perform(get("/api/docs/openapi.json"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);
        JsonNode document = mapper.readTree(content);

        assertEquals(
                "bearer",
                document.path("components")
                        .path("securitySchemes")
                        .path("bearer-jwt")
                        .path("scheme")
                        .asString());
        JsonNode apiError = document.path("components").path("schemas").path("ApiError");
        assertTrue(apiError.path("required").toString().contains("timestamp"));
        assertTrue(apiError.path("required").toString().contains("validationErrors"));

        JsonNode dashboard = document.path("paths").path("/api/v1/dashboard").path("get");
        assertFalse(dashboard.path("security").isEmpty());
        assertTrue(dashboard.path("responses").has("200"));
        assertTrue(dashboard.path("responses").has("400"));
        assertTrue(dashboard.path("responses").has("401"));
        assertTrue(dashboard.path("responses").has("403"));
        assertTrue(dashboard.path("responses").has("500"));
        String parameters = dashboard.path("parameters").toString();
        assertTrue(parameters.contains("\"name\":\"year\""));
        assertTrue(parameters.contains("\"name\":\"schoolId\""));

        JsonNode createResolution = document.path("paths").path("/api/v1/resolutions").path("post");
        assertFalse(createResolution.path("security").isEmpty());
        assertTrue(createResolution.path("requestBody").has("content"));
        assertTrue(createResolution.path("responses").has("201"));
        assertTrue(createResolution.path("responses").path("201").path("headers").has("Location"));
        assertTrue(createResolution.path("responses").has("409"));
    }
}

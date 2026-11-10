package com.claudecoders.grades.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claudecoders.grades.support.TestJwtSupport;
import com.claudecoders.grades.user.User;
import com.claudecoders.grades.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(AuthorizationIntegrationTest.JwtConfiguration.class)
class AuthorizationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @Test
    void authorizedIdentityCanUseProtectedReadAndWriteRoutes() throws Exception {
        User user = createUser("ADMIN_GT", true, "@unsa.edu.pe");
        String subject = user.getGoogleSubject();

        mvc.perform(get("/api/v1/auth/me").with(jwtFor(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.fullName").value(user.getFullName()))
                .andExpect(jsonPath("$.googleSubject").value(subject))
                .andExpect(jsonPath("$.role").value("ADMIN_GT"))
                .andExpect(jsonPath("$.active").value(true));

        mvc.perform(get("/api/v1/dashboard").with(jwtFor(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("America/Lima"))
                .andExpect(jsonPath("$.asOfDate").isString());
        mvc.perform(get("/api/v1/catalogs/schools").with(jwtFor(subject)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/catalogs/teachers").with(jwtFor(subject)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/expedients/search").with(jwtFor(subject)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/resolutions").with(jwtFor(subject))).andExpect(status().isOk());

        mvc.perform(
                        post("/api/v1/schools")
                                .with(jwtFor(subject))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Escuela auth " + subject + "\"}"))
                .andExpect(status().isCreated());

        User provisionedByEmail = new User();
        provisionedByEmail.setEmail("preprovisioned-" + subject + "@unsa.edu.pe");
        provisionedByEmail.setFullName("Usuario preaprovisionado");
        provisionedByEmail.setRole("ADMIN_GT");
        provisionedByEmail = users.saveAndFlush(provisionedByEmail);
        String provisionedEmail = provisionedByEmail.getEmail();
        mvc.perform(
                        get("/api/v1/auth/me")
                                .with(jwt().jwt(token -> token.claim("email", provisionedEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleSubject").isEmpty());
    }

    @Test
    void missingInvalidExpiredOrForeignTokensReturn401() throws Exception {
        User user = createUser("ADMIN_GT", true, "@unsa.edu.pe");
        String subject = user.getGoogleSubject();
        String expired = TestJwtSupport.token(subject, Instant.now().minusSeconds(60));
        String invalidSignature = TestJwtSupport.invalidSignatureToken(subject);
        String wrongIssuer =
                TestJwtSupport.token(
                        subject,
                        "https://issuer.example",
                        List.of(TestJwtSupport.AUDIENCE),
                        Instant.now().plusSeconds(3600));
        String wrongAudience =
                TestJwtSupport.token(
                        subject,
                        TestJwtSupport.ISSUER,
                        List.of("other-audience"),
                        Instant.now().plusSeconds(3600));

        List<MockHttpServletRequestBuilder> protectedRequests =
                List.of(
                        get("/api/v1/auth/me"),
                        get("/api/v1/dashboard"),
                        get("/api/v1/catalogs/schools"),
                        get("/api/v1/expedients/search"),
                        get("/api/v1/resolutions"),
                        get("/api/v1/resolutions/1"),
                        post("/api/v1/resolutions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"),
                        put("/api/v1/resolutions/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"),
                        delete("/api/v1/resolutions/1"),
                        put("/api/v1/expedients/1/academic")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"));

        for (MockHttpServletRequestBuilder request : protectedRequests) {
            mvc.perform(request).andExpect(status().isUnauthorized());
        }

        mvc.perform(get("/api/v1/dashboard").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/dashboard").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/v1/dashboard").header("Authorization", "Bearer " + invalidSignature))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/dashboard").header("Authorization", "Bearer " + wrongIssuer))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/dashboard").header("Authorization", "Bearer " + wrongAudience))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registeredButInactiveOrUnauthorizedIdentitiesReturn403() throws Exception {
        User unregistered = createUser("ADMIN_GT", true, "@unsa.edu.pe");
        User inactive = createUser("ADMIN_GT", false, "@unsa.edu.pe");
        User wrongRole = createUser("DOCENTE", true, "@unsa.edu.pe");
        User foreignEmail = createUser("ADMIN_GT", true, "@gmail.com");
        User authorized = createUser("ADMIN_GT", true, "@unsa.edu.pe");

        mvc.perform(
                        get("/api/v1/auth/me")
                                .with(jwtFor("subject-not-registered-" + unregistered.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/api/v1/auth/me").with(jwtFor(inactive.getGoogleSubject())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/dashboard").with(jwtFor(wrongRole.getGoogleSubject())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/auth/me").with(jwtFor(foreignEmail.getGoogleSubject())))
                .andExpect(status().isForbidden());

        String subject = authorized.getGoogleSubject();
        mvc.perform(
                        get("/api/v1/auth/me")
                                .with(
                                        jwt().jwt(
                                                        token ->
                                                                token.subject("different-subject")
                                                                        .claim(
                                                                                "email",
                                                                                authorized
                                                                                        .getEmail()))))
                .andExpect(status().isForbidden());
        mvc.perform(
                        get("/api/v1/dashboard")
                                .with(
                                        jwt().jwt(
                                                        token ->
                                                                token.subject(subject)
                                                                        .claim(
                                                                                "email",
                                                                                "personal@gmail.com"))))
                .andExpect(status().isForbidden());
        mvc.perform(
                        get("/api/v1/dashboard")
                                .with(
                                        jwt().jwt(
                                                        token ->
                                                                token.subject(subject)
                                                                        .claim("hd", "other.edu"))))
                .andExpect(status().isForbidden());

        List<MockHttpServletRequestBuilder> representativeWrites =
                List.of(
                        post("/api/v1/resolutions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"),
                        put("/api/v1/resolutions/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"),
                        delete("/api/v1/resolutions/1"),
                        put("/api/v1/expedients/1/academic")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"));
        for (MockHttpServletRequestBuilder request : representativeWrites) {
            mvc.perform(request.with(jwtFor(wrongRole.getGoogleSubject())))
                    .andExpect(status().isForbidden());
        }
    }

    private User createUser(String role, boolean active, String domain) {
        String suffix = Long.toUnsignedString(System.nanoTime());
        User user = new User();
        user.setEmail("auth-" + suffix + domain);
        user.setGoogleSubject("auth-subject-" + suffix);
        user.setFullName("Usuario auth " + suffix);
        user.setRole(role);
        user.setActive(active);
        return users.saveAndFlush(user);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(
            String subject) {
        return jwt().jwt(token -> token.subject(subject));
    }

    @TestConfiguration
    static class JwtConfiguration {
        @Bean
        JwtDecoder jwtDecoder() {
            return TestJwtSupport.decoder();
        }
    }
}

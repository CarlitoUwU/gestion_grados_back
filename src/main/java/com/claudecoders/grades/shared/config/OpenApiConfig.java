package com.claudecoders.grades.shared.config;

import com.claudecoders.grades.shared.exception.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Gestión de Grados y Títulos API")
                                .version("v1")
                                .description(
                                        "API versionada bajo /api/v1. La autenticación usa JWT Bearer"
                                                + " de Google y solo permite usuarios activos,"
                                                + " registrados, con rol autorizado y correo"
                                                + " institucional. El rol operativo actual es"
                                                + " ADMIN_GT. Las rutas de negocio rechazan un token"
                                                + " ausente, inválido o vencido con 401 y una identidad"
                                                + " no autorizada con 403."))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearer-jwt",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description(
                                                        "Google ID token JWT. Se validan firma,"
                                                                + " issuer, audience y expiración.")));
    }

    @Bean
    GlobalOpenApiCustomizer standardResponsesCustomizer() {
        return openApi -> {
            Components components =
                    openApi.getComponents() == null ? new Components() : openApi.getComponents();
            openApi.setComponents(components);
            ModelConverters.getInstance().readAll(ApiError.class).forEach(components::addSchemas);

            if (openApi.getPaths() == null) return;
            openApi.getPaths()
                    .forEach(
                            (path, pathItem) -> {
                                if (!path.startsWith("/api/v1")) return;
                                pathItem.readOperationsMap()
                                        .forEach(
                                                (method, operation) -> {
                                                    if (operation.getSecurity() == null
                                                            || operation.getSecurity().isEmpty()) {
                                                        operation.addSecurityItem(
                                                                new SecurityRequirement()
                                                                        .addList("bearer-jwt"));
                                                    }
                                                    ApiResponses responses =
                                                            operation.getResponses() == null
                                                                    ? new ApiResponses()
                                                                    : operation.getResponses();
                                                    operation.setResponses(responses);
                                                    addIfMissing(
                                                            responses,
                                                            "400",
                                                            "Datos inválidos o JSON mal formado",
                                                            operation.getRequestBody() != null);
                                                    addIfMissing(
                                                            responses,
                                                            "401",
                                                            "JWT ausente, inválido o vencido",
                                                            true);
                                                    addIfMissing(
                                                            responses,
                                                            "403",
                                                            "Identidad válida no registrada, inactiva o no autorizada",
                                                            true);
                                                    addIfMissing(
                                                            responses,
                                                            "404",
                                                            "Recurso inexistente",
                                                            path.contains("{"));
                                                    addIfMissing(
                                                            responses,
                                                            "409",
                                                            "Conflicto con datos existentes",
                                                            method != PathItem.HttpMethod.GET);
                                                    addIfMissing(
                                                            responses,
                                                            "500",
                                                            "Error interno",
                                                            true);
                                                });
                            });
        };
    }

    private static void addIfMissing(
            ApiResponses responses, String code, String description, boolean add) {
        if (!add || responses.containsKey(code)) return;
        responses.addApiResponse(code, errorResponse(description));
    }

    private static ApiResponse errorResponse(String description) {
        Schema<?> schema =
                new Schema<>().$ref("#/components/schemas/" + ApiError.class.getSimpleName());
        return new ApiResponse()
                .description(description)
                .content(
                        new Content()
                                .addMediaType("application/json", new MediaType().schema(schema)));
    }
}

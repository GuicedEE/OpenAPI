package com.guicedee.guicedservlets.openapi.implementations.test;

import com.guicedee.guicedservlets.openapi.implementations.OpenAPIProvider;
import com.jwebmp.guiced.openapi.tests.ProviderConfiguration;
import com.jwebmp.guiced.openapi.tests.ProviderResource;
import com.jwebmp.guiced.openapi.tests.ProviderMapperProcessor;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.function.UnaryOperator;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class OpenAPIProviderTest
{

    @AfterEach
    void resetConfiguration()
    {
        ProviderConfiguration.contribution = UnaryOperator.identity();
    }

    private SwaggerConfiguration resources(SwaggerConfiguration configuration)
    {
        configuration.setResourceClasses(Set.of(ProviderResource.class.getName()));
        return configuration;
    }

    @Test
    void generates31EagerlyWithJsonAndYamlSchemas()
    {
        ProviderConfiguration.contribution = c -> resources((SwaggerConfiguration)c);
        OpenAPIProvider provider = new OpenAPIProvider();
        OpenAPI model = provider.get();
        assertSame(model, provider.get());
        assertEquals("3.1.0", model.getOpenapi());
        assertNotNull(model.getPaths().get("/provider/unannotated"));
        var context = OpenApiContextLocator.getInstance().getOpenApiContext("context");
        assertSame(model, context.read());
        assertEquals(Boolean.FALSE, context.getOpenApiConfiguration().isConvertToOpenAPI31());
        for (var mapper : new tools.jackson.databind.ObjectMapper[]{context.getOutputJsonMapper(), context.getOutputYamlMapper()})
        {
            OpenAPI decoded = mapper.readValue(mapper.writeValueAsString(model), OpenAPI.class);
            assertEquals("3.1.0", decoded.getOpenapi());
            var json = io.swagger.v3.core.util.Json31.mapper().valueToTree(decoded);
            assertEquals("#/components/schemas/ProviderMapResponse",
                    json.at("/paths/~1provider/get/responses/200/content/application~1json/schema/$ref").asText());
            assertEquals("#/components/schemas/ProviderMapValue",
                    json.at("/components/schemas/ProviderMapResponse/properties/entries/additionalProperties/$ref").asText());
        }
    }

    @Test
    void readerHonorsSpiConfiguration()
    {
        ProviderConfiguration.contribution = c -> resources((SwaggerConfiguration)c).readAllResources(false);
        OpenAPI model = new OpenAPIProvider().get();
        assertNotNull(model.getPaths().get("/provider"));
        assertNull(model.getPaths().get("/provider/unannotated"));
    }

    @Test
    void spiCanReplaceConfigurationAndSelect30()
    {
        OpenAPI seed = new OpenAPI().info(new Info().title("SPI metadata").version("1"));
        ProviderConfiguration.contribution = c -> resources(new SwaggerConfiguration())
                .openAPI31(false).convertToOpenAPI31(false).readAllResources(true)
                .openAPI(seed);
        OpenAPI model = new OpenAPIProvider().get();
        assertEquals("3.0.1", model.getOpenapi());
        assertEquals("SPI metadata", model.getInfo().getTitle());
        assertNotNull(model.getPaths().get("/provider"));
        assertNull(seed.getPaths(), "The caller-owned seed model must not be modified");
    }

    @Test
    void customMapperProcessorAndSortedOutputStillUseSwaggerInitialization()
    {
        ProviderMapperProcessor.calls.clear();
        ProviderConfiguration.contribution = c -> resources((SwaggerConfiguration)c)
                .objectMapperProcessorClass(ProviderMapperProcessor.class.getName()).sortOutput(true);
        OpenAPI model = new OpenAPIProvider().get();
        assertEquals(Set.of("model", "json", "yaml"), ProviderMapperProcessor.calls);
        var context = OpenApiContextLocator.getInstance().getOpenApiContext("context");
        assertTrue(context.getOutputJsonMapper().isEnabled(tools.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
        assertTrue(context.getOutputYamlMapper().isEnabled(tools.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
        assertNotNull(model.getPaths().get("/provider"));
    }

    @Test
    void legacyConversionRemainsOptIn()
    {
        ProviderConfiguration.contribution = c -> resources((SwaggerConfiguration)c).openAPI31(false).convertToOpenAPI31(true);
        OpenAPI model = new OpenAPIProvider().get();
        assertEquals("3.1.0", model.getOpenapi());
        assertNotNull(model.getPaths().get("/provider"));
    }

    @Test
    void customResponseCodeReachesReader()
    {
        ProviderConfiguration.contribution = c -> resources((SwaggerConfiguration)c).defaultResponseCode("202");
        var model = new OpenAPIProvider().get();
        var json = io.swagger.v3.core.util.Json31.mapper().valueToTree(model);
        assertFalse(json.at("/paths/~1provider~1unannotated/get/responses/202").isMissingNode());
    }

    @Test
    void fileConfigurationIsAppliedBeforeSpi() throws Exception
    {
        Path file = Path.of("openapi-configuration.json");
        assumeFalse(Files.exists(file), "Do not replace an existing application configuration");
        Files.writeString(file, "{\"openAPI31\":true,\"readAllResources\":false,\"openAPI\":{\"info\":{\"title\":\"File metadata\",\"version\":\"1\"}}}", StandardOpenOption.CREATE_NEW);
        try
        {
            ProviderConfiguration.contribution = c -> {
                assertEquals("File metadata", c.getOpenAPI().getInfo().getTitle());
                c.getOpenAPI().getInfo().setTitle("SPI override");
                return resources((SwaggerConfiguration)c);
            };
            OpenAPI model = new OpenAPIProvider().get();
            assertEquals("SPI override", model.getInfo().getTitle());
            assertEquals("3.1.0", model.getOpenapi());
            assertNull(model.getPaths().get("/provider/unannotated"));
        }
        finally { Files.delete(file); }
    }

    @Test
    void nullSpiConfigurationFailsClearly()
    {
        ProviderConfiguration.contribution = c -> null;
        assertTrue(assertThrows(NullPointerException.class, OpenAPIProvider::new)
                .getMessage().contains(ProviderConfiguration.class.getName()));
    }
}

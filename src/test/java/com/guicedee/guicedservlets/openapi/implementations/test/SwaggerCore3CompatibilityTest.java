package com.guicedee.guicedservlets.openapi.implementations.test;

import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.rest.Reader;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Exercises the official REST reader and Jackson 3 schema codecs in the named wrapper. */
class SwaggerCore3CompatibilityTest
{
    @Test
    void jsonPreservesResponseReferencesAndMapSchemas()
    {
        assertRoundTrip(Json31.mapper());
    }

    @Test
    void yamlPreservesResponseReferencesAndMapSchemas()
    {
        assertRoundTrip(Yaml31.mapper());
    }

    private void assertRoundTrip(ObjectMapper mapper)
    {
        SwaggerConfiguration configuration = new SwaggerConfiguration();
        configuration.setOpenAPI31(true);
        configuration.setReadAllResources(true);
        OpenAPI generated = new Reader(configuration).read(MapResource.class);
        assertNotNull(generated.getPaths().get("/swagger-compatibility"));

        String encoded = mapper.writeValueAsString(generated);
        OpenAPI decoded = mapper.readValue(encoded, OpenAPI.class);
        // Inspect through Swagger's JSON codec so both JSON and YAML exercise the same contract.
        var json = Json31.mapper().readTree(Json31.mapper().writeValueAsString(decoded));
        assertEquals("#/components/schemas/MapResponse",
                json.at("/paths/~1swagger-compatibility/get/responses/200/content/application~1json/schema/$ref").asText());
        assertEquals("#/components/schemas/MapValue",
                json.at("/components/schemas/MapResponse/properties/entries/additionalProperties/$ref").asText());
        assertEquals("string", json.at("/components/schemas/MapValue/properties/value/type").asText());
    }

    @Path("/swagger-compatibility")
    public static class MapResource
    {
        @GET
        @Produces("application/json")
        @ApiResponse(responseCode = "200", description = "Typed map",
                content = @Content(schema = @Schema(implementation = MapResponse.class)))
        public MapResponse get()
        {
            return new MapResponse();
        }
    }

    @Schema(name = "MapResponse")
    public static class MapResponse
    {
        public Map<String, MapValue> entries;
    }

    @Schema(name = "MapValue")
    public static class MapValue
    {
        public String value;
    }
}

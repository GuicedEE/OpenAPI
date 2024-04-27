package com.guicedee.guicedservlets.openapi.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("openapi.json")
@Produces("text/json")
public class OpenAPIJsonEndpoint
{
    @Inject
    OpenAPI openAPI;

    @GET
    public String renderOpenAPI()
    {
        OpenApiContext context = OpenApiContextLocator.getInstance()
                                                      .getOpenApiContext("context");
        try
        {
            return context.getOutputJsonMapper()
                          .writerWithDefaultPrettyPrinter()
                          .writeValueAsString(openAPI);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }

    }
}

package com.guicedee.guicedservlets.openapi.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.web.spi.VertxRouterConfigurator;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;

public class OpenAPIRouter implements VertxRouterConfigurator
{
    @Override
    public Router builder(Router builder)
    {
        OpenApiContext openApiContext = OpenApiContextLocator.getInstance()
                .getOpenApiContext("context");

        builder.get("/openapi.json")
                .produces("text/json")
                .enable()
                .handler(context -> {
                    var openAPI = IGuiceContext.get(OpenAPI.class);
                    String output = null;
                    try
                    {
                        output = openApiContext.getOutputJsonMapper()
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(openAPI);
                    }
                    catch (JsonProcessingException e)
                    {
                        throw new RuntimeException(e);
                    }
                    context.response()
                            .setStatusCode(200)
                            .end(output)
                            ;
                });

        builder.get("/openapi.yaml")
                .produces("application/yaml")
                .enable()
                .handler(context -> {
                    var openAPI = IGuiceContext.get(OpenAPI.class);
                    String output = null;
                    try
                    {
                        output = openApiContext.getOutputYamlMapper()
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(openAPI);
                    }
                    catch (JsonProcessingException e)
                    {
                        throw new RuntimeException(e);
                    }
                    context.response()
                            .setStatusCode(200)
                            .end(output);
                });

        return builder;
    }
}

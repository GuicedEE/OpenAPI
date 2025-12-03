package com.guicedee.guicedservlets.openapi.implementations;

import com.google.inject.AbstractModule;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.services.jsonrepresentation.IJsonRepresentation;
import io.swagger.v3.core.jackson.SwaggerModule;
import io.swagger.v3.oas.models.OpenAPI;

public class OpenAPIModule extends AbstractModule implements IGuiceModule<OpenAPIModule>
{
    @Override
    protected void configure()
    {
        IJsonRepresentation.getObjectMapper()
                           .registerModule(new SwaggerModule());

        bind(OpenAPI.class).toProvider(OpenAPIProvider.class).asEagerSingleton();
    }
}

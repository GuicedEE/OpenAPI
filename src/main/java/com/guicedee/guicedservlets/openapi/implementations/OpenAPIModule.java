package com.guicedee.guicedservlets.openapi.implementations;

import com.google.inject.AbstractModule;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.services.jsonrepresentation.IJsonRepresentation;
import io.swagger.v3.core.jackson.SwaggerModule;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * Guice module that wires the OpenAPI runtime pieces into the application.
 *
 * <p>It registers the Swagger Jackson module on the shared JSON representation
 * mapper and binds the {@link OpenAPI} model to a singleton provider that reads
 * the application resources once at startup.</p>
 */
public class OpenAPIModule extends AbstractModule implements IGuiceModule<OpenAPIModule>
{
    /**
     * Registers Swagger Jackson support and binds the OpenAPI provider.
     */
    @Override
    protected void configure()
    {
        IJsonRepresentation.getObjectMapper()
                           .registerModule(new SwaggerModule());

        bind(OpenAPI.class).toProvider(OpenAPIProvider.class).asEagerSingleton();
    }
}

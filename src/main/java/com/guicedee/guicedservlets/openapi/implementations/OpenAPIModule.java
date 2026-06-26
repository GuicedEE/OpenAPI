package com.guicedee.guicedservlets.openapi.implementations;

import com.google.inject.AbstractModule;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * Guice module that wires the OpenAPI runtime pieces into the application.
 *
 * <p>It binds the {@link OpenAPI} model to a singleton provider that reads
 * the application resources once at startup. OpenAPI serialization is handled
 * by Swagger's own {@code Json31}/context mappers (which already register the
 * Swagger Jackson module), so the shared immutable Jackson 3 mapper does not
 * need to be mutated here.</p>
 */
public class OpenAPIModule extends AbstractModule implements IGuiceModule<OpenAPIModule>
{
    /**
     * Binds the OpenAPI provider.
     */
    @Override
    protected void configure()
    {
        bind(OpenAPI.class).toProvider(OpenAPIProvider.class).asEagerSingleton();
    }
}

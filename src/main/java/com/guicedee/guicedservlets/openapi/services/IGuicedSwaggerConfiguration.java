package com.guicedee.guicedservlets.openapi.services;


import com.guicedee.client.services.IDefaultService;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;

/**
 * Service provider interface that allows modules to customize OpenAPI configuration.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader} and
 * invoked during {@link com.guicedee.guicedservlets.openapi.implementations.OpenAPIProvider}
 * construction. Each implementation can mutate or replace the provided
 * configuration before the OpenAPI context is initialized.</p>
 */
@FunctionalInterface
public interface IGuicedSwaggerConfiguration
		extends IDefaultService<IGuicedSwaggerConfiguration>
{
	/**
	 * Applies additional configuration to the shared OpenAPI configuration.
	 *
	 * @param config the current configuration instance
	 *
	 * @return the updated configuration instance to use
	 */
	OpenAPIConfiguration config(OpenAPIConfiguration config);
}

package com.guicedee.guicedservlets.openapi.services;


import com.guicedee.guicedinjection.interfaces.IDefaultService;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;

/**
 * SPI that allows multi-module configuration for swagger
 */
@FunctionalInterface
public interface IGuicedSwaggerConfiguration
		extends IDefaultService<IGuicedSwaggerConfiguration>
{
	/**
	 * Passes through the swagger configuration properties
	 *
	 * @param config
	 *
	 * @return
	 */
	OpenAPIConfiguration config(OpenAPIConfiguration config);
}

package com.guicedee.guicedservlets.openapi.services;


import com.guicedee.guicedinjection.interfaces.IDefaultService;
import com.guicedee.guicedservlets.openapi.OpenAPIModule;

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
	OpenAPIModule config(OpenAPIModule config);
}

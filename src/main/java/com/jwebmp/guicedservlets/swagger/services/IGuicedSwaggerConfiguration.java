package com.guicedee.guicedservlets.swagger.services;


import com.guicedee.guicedservlets.swagger.GuicedSwaggerConfig;
import com.guicedee.guicedinjection.interfaces.IDefaultService;
import io.swagger.v3.oas.models.info.Info;

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
	 * @param info
	 *
	 * @return
	 */
	GuicedSwaggerConfig config(GuicedSwaggerConfig config, Info info);
}

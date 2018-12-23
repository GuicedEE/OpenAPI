package com.jwebmp.guiced.swagger.services;


import com.jwebmp.guiced.swagger.GuicedSwaggerConfig;
import com.jwebmp.guicedinjection.interfaces.IDefaultService;
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

package com.jwebmp.guiced.swagger;

import io.swagger.v3.oas.integration.SwaggerConfiguration;

public class GuicedSwaggerConfig<J extends GuicedSwaggerConfig<J>>
{

	private SwaggerConfiguration configuration;

	public GuicedSwaggerConfig()
	{
		//Not Used
	}

	public SwaggerConfiguration getConfiguration()
	{
		return configuration;
	}

	public GuicedSwaggerConfig<J> setConfiguration(SwaggerConfiguration configuration)
	{
		this.configuration = configuration;
		return this;
	}
}

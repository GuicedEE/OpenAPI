package com.jwebmp.guiced.swagger;

import com.jwebmp.guiced.rest.internal.RestEasyPackageRegistrations;
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
		if (configuration != null)
		{
			if (configuration.getResourcePackages() == null)
			{
				configuration.setResourcePackages(RestEasyPackageRegistrations.getPackageNames());
			}
			else
			{
				configuration.getResourcePackages()
				             .addAll(RestEasyPackageRegistrations.getPackageNames());
			}
		}
		return this;
	}
}

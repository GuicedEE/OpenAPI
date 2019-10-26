package com.guicedee.guicedservlets.swagger;

import com.guicedee.guicedservlets.rest.internal.JaxRsPackageRegistrations;
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
				configuration.setResourcePackages(JaxRsPackageRegistrations.getPackageNames());
			}
			else
			{
				configuration.getResourcePackages()
				             .addAll(JaxRsPackageRegistrations.getPackageNames());
			}
		}
		return this;
	}
}

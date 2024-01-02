package com.guicedee.guicedservlets.openapi;

import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedservlets.rest.annotations.RestFeature;
import com.guicedee.guicedservlets.openapi.services.IGuicedSwaggerConfiguration;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;

import java.util.ServiceLoader;
import java.util.Set;

/**
 * Configures CXF OpenAPI for GuicedEE
 *
 * <a href="https://cxf.apache.org/docs/openapifeature.html">CXF Source Documentation</a>
 */
@RestFeature("com.guicedee.guicedservlets.openapi.OpenAPIModule")
public class OpenAPIModule extends OpenApiFeature
{
	public OpenAPIModule()
	{
		setScan(true);
		Set<IGuicedSwaggerConfiguration> configurations = GuiceContext.instance().loaderToSet(ServiceLoader.load(IGuicedSwaggerConfiguration.class));
		for (IGuicedSwaggerConfiguration configuration : configurations)
		{
			configuration.config(this);
		}
	}
}

package com.guicedee.guicedservlets.swagger.implementations;

import com.google.inject.Singleton;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedinjection.interfaces.IDefaultService;
import com.guicedee.guicedservlets.rest.RESTContext;
import com.guicedee.guicedservlets.swagger.GuicedSwaggerConfig;
import com.guicedee.guicedservlets.swagger.services.IGuicedSwaggerConfiguration;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.OpenApiServlet;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.util.ServiceLoader;
import java.util.Set;

@Singleton public class SwaggerServlet extends OpenApiServlet {
	@Override public void init(ServletConfig config) throws ServletException {
		OpenAPI oas = new OpenAPI();

		Set<IGuicedSwaggerConfiguration> services = IDefaultService.loaderToSet(ServiceLoader.load(IGuicedSwaggerConfiguration.class));

		GuicedSwaggerConfig<?> oasConfig = new GuicedSwaggerConfig<>();
		oasConfig.setConfiguration(new SwaggerConfiguration());

		Info info = new Info();
		for (IGuicedSwaggerConfiguration service : services) {
			oasConfig = service.config(oasConfig, info);
		}
		oas.info(info);
		try {
			new JaxrsOpenApiContextBuilder().servletConfig(config).openApiConfiguration(oasConfig.getConfiguration()).resourcePackages(RESTContext.getPathServices())
					.buildContext(true);
		} catch (OpenApiConfigurationException e) {
			throw new ServletException(e.getMessage(), e);
		}
	}
}

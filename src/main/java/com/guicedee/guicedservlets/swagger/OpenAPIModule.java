package com.guicedee.guicedservlets.swagger;

import com.guicedee.guicedservlets.services.GuiceSiteInjectorModule;
import com.guicedee.guicedservlets.services.IGuiceSiteBinder;
import com.guicedee.guicedservlets.swagger.implementations.SwaggerServlet;

import java.util.HashMap;
import java.util.Map;

public class OpenAPIModule
		implements IGuiceSiteBinder<GuiceSiteInjectorModule>
{
	private static String path = "/openapi";

	public OpenAPIModule()
	{
		//Not required
	}

	public OpenAPIModule(final String path)
	{
		this.path = path;
	}

	public static String getPath()
	{
		return path;
	}

	public static void setPath(String path)
	{
		OpenAPIModule.path = path;
	}

	@Override
	public void onBind(GuiceSiteInjectorModule module)
	{
		Map<String, String> props = new HashMap<>();
		props.put("jersey.config.server.wadl.disableWadl", "true");
		props.put("jersey.config.server.provider.packages", "");
		module.serve$(path + "/*")
		      .with(SwaggerServlet.class, props);
	}

}

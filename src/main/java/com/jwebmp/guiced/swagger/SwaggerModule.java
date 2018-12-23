package com.jwebmp.guiced.swagger;

import com.jwebmp.guiced.swagger.implementations.SwaggerServlet;
import com.jwebmp.guicedservlets.services.GuiceSiteInjectorModule;
import com.jwebmp.guicedservlets.services.IGuiceSiteBinder;

import java.util.HashMap;
import java.util.Map;


public class SwaggerModule
		implements IGuiceSiteBinder<GuiceSiteInjectorModule>
{

	private static String path = "/swagger";

	public SwaggerModule()
	{
		//Not required
	}

	public SwaggerModule(final String path)
	{
		this.path = path;
	}

	@Override
	public void onBind(GuiceSiteInjectorModule module)
	{
		Map<String, String> props = new HashMap<>();
		props.put("jersey.config.server.wadl.disableWadl", "true");
		module.serve$(path + "/*").with(SwaggerServlet.class, props);

	}

	public static String getPath()
	{
		return path;
	}

	public static void setPath(String path)
	{
		SwaggerModule.path = path;
	}
}

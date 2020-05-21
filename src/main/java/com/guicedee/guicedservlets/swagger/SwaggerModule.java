package com.guicedee.guicedservlets.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.guicedee.guicedservlets.swagger.implementations.SwaggerServlet;
import com.guicedee.guicedservlets.swagger.jsonoverrides.Json;
import com.guicedee.guicedservlets.services.GuiceSiteInjectorModule;
import com.guicedee.guicedservlets.services.IGuiceSiteBinder;

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

	public static String getPath()
	{
		return path;
	}

	public static void setPath(String path)
	{
		SwaggerModule.path = path;
	}

	@Override
	public void onBind(GuiceSiteInjectorModule module)
	{
		Json.mapper()
		    .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
		    .enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES)
		    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
		    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
		    .disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS);

		Map<String, String> props = new HashMap<>();
		props.put("jersey.config.server.wadl.disableWadl", "true");
		props.put("jersey.config.server.provider.packages", "");
		module.serve$(path + "/*")
		      .with(SwaggerServlet.class, props);
	}

}

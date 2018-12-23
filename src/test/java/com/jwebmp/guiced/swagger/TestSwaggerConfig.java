package com.jwebmp.guiced.swagger;

import com.jwebmp.guiced.swagger.services.IGuicedSwaggerConfiguration;
import io.swagger.v3.oas.models.info.Info;

import java.util.HashSet;
import java.util.Set;

public class TestSwaggerConfig implements IGuicedSwaggerConfiguration
{
	@Override
	public GuicedSwaggerConfig config(GuicedSwaggerConfig config, Info info)
	{
		Set<String> packages = new HashSet<>();
		packages.add("com.jwebmp.guiced.swagger");
		config.getConfiguration().setResourcePackages(packages);
		return config;
	}
}

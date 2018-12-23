import com.jwebmp.guiced.swagger.SwaggerModule;
import com.jwebmp.guiced.swagger.services.IGuicedSwaggerConfiguration;
import com.jwebmp.guicedservlets.services.IGuiceSiteBinder;

module com.jwebmp.guiced.swagger {
	exports com.jwebmp.guiced.swagger;

	requires com.jwebmp.guicedservlets;

	requires javax.servlet.api;
	requires swagger.core;
	requires swagger.annotations;
	requires swagger.integration;
	requires swagger.models;
	requires swagger.jaxrs2;
	requires com.jwebmp.guiced.rest;

	uses IGuicedSwaggerConfiguration;

	provides IGuiceSiteBinder with SwaggerModule;

}

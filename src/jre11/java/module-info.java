module com.jwebmp.guiced.swagger {
	exports com.jwebmp.guiced.swagger;

	requires com.jwebmp.guicedservlets;

	requires javax.servlet.api;
	requires com.jwebmp.guiced.rest;
	requires io.swagger.v3.oas.integration;
	requires io.swagger.v3.jaxrs2;
	requires io.swagger.v3.oas.models;

	uses com.jwebmp.guiced.swagger.services.IGuicedSwaggerConfiguration;

	provides com.jwebmp.guicedservlets.services.IGuiceSiteBinder with com.jwebmp.guiced.swagger.SwaggerModule;

}

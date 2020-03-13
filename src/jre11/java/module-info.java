module com.guicedee.guicedservlets.swagger {
	exports com.guicedee.guicedservlets.swagger;

	requires com.guicedee.guicedservlets;

	requires javax.servlet.api;
	requires com.guicedee.guicedservlets.rest;

	requires com.fasterxml.jackson.datatype.jdk8;

	requires static io.swagger.v3.oas.integration;
	requires static io.swagger.v3.jaxrs2;
	requires static io.swagger.v3.oas.models;

	requires org.apache.commons.lang3;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.datatype.jsr310;
	requires io.github.classgraph;

	requires java.ws.rs;

	requires com.fasterxml.jackson.jaxrs.json;

	//shaded
	requires static io.swagger.v3.core;

	uses com.guicedee.guicedservlets.swagger.services.IGuicedSwaggerConfiguration;

	exports com.guicedee.guicedservlets.swagger.services;
	//opens io.swagger.v3.jaxrs2.integration.resources to org.apache.cxf;

	provides com.guicedee.guicedservlets.services.IGuiceSiteBinder with com.guicedee.guicedservlets.swagger.SwaggerModule;

	//uses io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
	//uses io.swagger.v3.core.converter.ModelConverter;
}

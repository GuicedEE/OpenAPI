module com.guicedee.guicedservlets.swagger {
	exports com.guicedee.guicedservlets.swagger;

	exports io.swagger.v3.oas.annotations;
	exports io.swagger.v3.oas.annotations.media;
	exports io.swagger.v3.oas.annotations.responses;
	exports io.swagger.v3.oas.annotations.info;
	exports io.swagger.v3.oas.annotations.tags;
	exports io.swagger.v3.oas.annotations.extensions;
	exports io.swagger.v3.oas.annotations.headers;
	exports io.swagger.v3.oas.annotations.links;
	exports io.swagger.v3.oas.annotations.servers;
	exports io.swagger.v3.oas.annotations.security;
	exports io.swagger.v3.oas.annotations.parameters;

	exports com.guicedee.guicedservlets.swagger.services;
	exports io.swagger.v3.oas.models.info;
	exports io.swagger.v3.oas.models.servers;
	exports io.swagger.v3.oas.annotations.enums;
	exports io.swagger.v3.oas.models.security;

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
	requires com.fasterxml.jackson.jaxrs.json;
	requires io.github.classgraph;
	requires java.ws.rs;

	//shaded
	requires static io.swagger.v3.core;

	uses com.guicedee.guicedservlets.swagger.services.IGuicedSwaggerConfiguration;

	opens com.guicedee.guicedservlets.swagger to com.google.guice;
	opens com.guicedee.guicedservlets.swagger.implementations to com.google.guice;
	opens io.swagger.v3.jaxrs2.integration.resources to com.google.guice, org.apache.cxf;
	//opens com.fasterxml.jackson.jaxrs.json to com.google.guice, org.apache.cxf;
	opens io.swagger.v3.jaxrs2 to com.google.guice, org.apache.cxf;

	opens io.swagger.v3.core.jackson to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models.parameters to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models.media to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models.responses to com.fasterxml.jackson.databind;

	provides com.guicedee.guicedservlets.services.IGuiceSiteBinder with com.guicedee.guicedservlets.swagger.SwaggerModule;

	uses io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
	uses io.swagger.v3.core.converter.ModelConverter;
	uses io.swagger.v3.oas.integration.api.OpenAPIConfigBuilder;
}

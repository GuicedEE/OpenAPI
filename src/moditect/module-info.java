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

	requires transitive com.guicedee.guicedservlets.rest;

	uses com.guicedee.guicedservlets.swagger.services.IGuicedSwaggerConfiguration;

	opens com.guicedee.guicedservlets.swagger to com.google.guice;
	opens com.guicedee.guicedservlets.swagger.implementations to com.google.guice;
	opens io.swagger.v3.jaxrs2.integration.resources to com.google.guice, org.apache.cxf;
	opens io.swagger.v3.jaxrs2 to com.google.guice, org.apache.cxf;

	opens io.swagger.v3.oas.integration to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.annotations to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.annotations.media to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.annotations.responses to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.annotations.info to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.annotations.tags to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.annotations.extensions to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.annotations.headers to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.annotations.links to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.annotations.servers to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.annotations.security to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.annotations.parameters to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.annotations.enums to com.fasterxml.jackson.databind;

	opens io.swagger.v3.core.jackson to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models.parameters to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models.media to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models.responses to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models.tags to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models.info to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models.headers to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models.callbacks to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models.links to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models.security to com.fasterxml.jackson.databind;
	opens io.swagger.v3.oas.models.servers to com.fasterxml.jackson.databind;

	provides com.guicedee.guicedservlets.services.IGuiceSiteBinder with com.guicedee.guicedservlets.swagger.SwaggerModule;

	uses io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
	uses io.swagger.v3.core.converter.ModelConverter;
	uses io.swagger.v3.oas.integration.api.OpenAPIConfigBuilder;
}

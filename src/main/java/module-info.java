import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.client.services.config.IGuiceScanModuleInclusions;
import com.guicedee.guicedservlets.openapi.implementations.IncludeModuleInScans;
import com.guicedee.guicedservlets.openapi.implementations.OpenAPIModule;
import com.guicedee.guicedservlets.openapi.implementations.OpenAPIRouter;

/**
 * Integrates OpenAPI generation with Guice, providing a scanner, provider, and
 * Vertx router endpoints for JSON/YAML OpenAPI documents.
 *
 * <p>This module exports the service SPI for configuration and wires default
 * implementations through the standard Guice/ServiceLoader facilities.</p>
 */
module com.guicedee.openapi {

	requires transitive com.fasterxml.jackson.databind;
	requires transitive com.guicedee.vertx.web;
	requires transitive com.guicedee.client;

	requires static lombok;
    requires transitive com.guicedee.services.openapi;

    requires org.apache.commons.lang3;
	requires com.guicedee.jsonrepresentation;


	exports com.guicedee.guicedservlets.openapi.services;

	uses com.guicedee.guicedservlets.openapi.services.IGuicedSwaggerConfiguration;
	
	provides IGuiceScanModuleInclusions with IncludeModuleInScans;
	provides IGuiceModule with OpenAPIModule;
	provides com.guicedee.vertx.web.spi.VertxRouterConfigurator with OpenAPIRouter;

	opens com.guicedee.guicedservlets.openapi.services to com.google.guice;
	opens com.guicedee.guicedservlets.openapi.implementations to com.google.guice;
    exports com.guicedee.guicedservlets.openapi.implementations to guiced.openapi.tests;
}

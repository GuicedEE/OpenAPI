import com.guicedee.guicedservlets.openapi.implementations.IncludeModuleInScans;
import com.guicedee.guicedservlets.openapi.implementations.OpenAPIModule;
import com.guicedee.guicedinjection.interfaces.*;

module com.guicedee.openapi {

	requires transitive com.fasterxml.jackson.databind;
	
	requires transitive com.guicedee.rest;
	requires transitive com.guicedee.client;

	requires static lombok;
    requires transitive com.guicedee.services.openapi;

    requires org.apache.commons.lang3;
    requires jakarta.validation;
	requires com.zandero.rest.vertx;
	requires com.guicedee.jsonrepresentation;

	exports com.guicedee.guicedservlets.openapi.services;
	uses com.guicedee.guicedservlets.openapi.services.IGuicedSwaggerConfiguration;
	
	provides IGuiceScanModuleInclusions with IncludeModuleInScans;
	provides IGuiceModule with OpenAPIModule;

	opens com.guicedee.guicedservlets.openapi.services to com.google.guice;
	opens com.guicedee.guicedservlets.openapi.implementations to com.google.guice,io.vertx.rest;
    exports com.guicedee.guicedservlets.openapi.implementations to guiced.openapi.tests;
}

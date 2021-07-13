module com.guicedee.guicedservlets.openapi {
	requires transitive com.guicedee.services.openapi;
	requires transitive com.guicedee.guicedservlets.rest;
	
	exports com.guicedee.guicedservlets.swagger;
	exports com.guicedee.guicedservlets.swagger.services;
	
	uses com.guicedee.guicedservlets.swagger.services.IGuicedSwaggerConfiguration;
	
	opens com.guicedee.guicedservlets.swagger to com.google.guice;
	opens com.guicedee.guicedservlets.swagger.implementations to com.google.guice;
	
	provides com.guicedee.guicedservlets.services.IGuiceSiteBinder with com.guicedee.guicedservlets.swagger.OpenAPIModule;
	
}

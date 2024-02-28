import com.guicedee.guicedinjection.interfaces.IGuiceScanModuleInclusions;
import com.guicedee.guicedservlets.openapi.implementations.IncludeModuleInScans;

module com.guicedee.guicedservlets.openapi {
	requires org.apache.cxf.rest.openapi;
	
	requires org.apache.cxf;
	requires org.apache.cxf.rest;
	
	requires transitive com.guicedee.guicedservlets.rest;
	requires com.guicedee.client;
	
	exports com.guicedee.guicedservlets.openapi;
	exports com.guicedee.guicedservlets.openapi.services;
	
	uses com.guicedee.guicedservlets.openapi.services.IGuicedSwaggerConfiguration;
	
	provides IGuiceScanModuleInclusions with IncludeModuleInScans;
	
	opens com.guicedee.guicedservlets.openapi to com.google.guice;
	opens com.guicedee.guicedservlets.openapi.services to com.google.guice;
}

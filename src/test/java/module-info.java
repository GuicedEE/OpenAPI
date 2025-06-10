
import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import com.jwebmp.guiced.openapi.tests.RestTestBinding;

module guiced.openapi.tests {
	
	requires com.guicedee.openapi;
	
	requires java.net.http;
	
	requires org.junit.jupiter.api;
	//requires org.slf4j;
	//requires org.slf4j.simple;
	requires jakarta.ws.rs;

    requires com.guicedee.services.openapi;
    requires com.google.guice;
    requires com.guicedee.client;

    provides IGuiceModule with RestTestBinding;

	opens com.guicedee.guicedservlets.openapi.implementations.test to org.junit.platform.commons,com.fasterxml.jackson.databind,com.google.guice;
	opens com.jwebmp.guiced.openapi.tests to org.junit.platform.commons,com.fasterxml.jackson.databind,com.google.guice,com.zandero.rest.vertx;

}
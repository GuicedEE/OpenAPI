import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import com.jwebmp.guiced.openapi.tests.RestTestBinding;

module guiced.openapi.tests {
	
	requires com.guicedee.guicedservlets.openapi;
	
	requires java.net.http;
	
	requires org.junit.jupiter.api;
	requires org.slf4j;
	requires org.slf4j.simple;
	requires jakarta.ws.rs;
	requires com.guicedee.guicedservlets.undertow;
	
	provides IGuiceModule with RestTestBinding;
	
}
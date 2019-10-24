package com.guicedee.guiced.swagger;

import io.undertow.Undertow;
import org.junit.jupiter.api.Test;
import com.guicedee.guicedservlets.undertow.GuicedUndertow;
import com.guicedee.logger.LogFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class RestEasyModuleTest
{

	@Test
	public void configureServlets() throws Exception
	{
		LogFactory.configureConsoleColourOutput(Level.FINE);
		Undertow undertow = GuicedUndertow.boot("0.0.0.0", 6004);

		//Do stuff
		HttpClient client = HttpClient.newBuilder()
		                              .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
		                              .build();
		HttpResponse response = client.send(HttpRequest.newBuilder()
		                                               .GET()
		                                               .uri(new URI("http://localhost:6004/rest/hello/world"))
		                                               .build(),
		                                    HttpResponse.BodyHandlers.discarding());

		assertEquals(200, response.statusCode());

		response = client.send(HttpRequest.newBuilder()
		                                  .GET()
		                                  .uri(new URI("http://localhost:6004/swagger/"))
		                                  .build(),
		                       HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode(), "Hello World Rest not available");
		String resp = response.body()
		                      .toString();
		if (!resp.contains("\"openapi\":"))
		{
			fail("Open API Swagger not available");
		}
		//undertow.stop();
	}

	public static void main(String[] args) throws Exception
	{
		new RestEasyModuleTest().configureServlets();
	}
}

package com.jwebmp.guiced.openapi.tests;

import com.guicedee.guicedservlets.undertow.GuicedUndertow;
import io.undertow.Undertow;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class RestEasyModuleTest
{

	@Test
	public void configureServlets() throws Exception
	{
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
		                                  .uri(new URI("http://localhost:6004/rest/openapi.json"))
		                                  .build(),
		                       HttpResponse.BodyHandlers.ofString());
		
		System.out.println("Response from openapi.json - " + response);
		System.out.println("Response from openapi.json - " + response.body());

		assertEquals(200, response.statusCode(), "Hello World Rest not available");
		String resp = response.body()
		                      .toString();
		
		if (!resp.contains("\"openapi\" :"))
		{
			fail("Open API Swagger not available");
		}
		
		response = client.send(HttpRequest.newBuilder()
										.GET()
										.uri(new URI("http://localhost:6004/rest/openapi.yaml"))
										.build(),
						HttpResponse.BodyHandlers.ofString());
		
		System.out.println("Response from openapi.yaml - " + response);
		System.out.println("Response from openapi.yaml - " + response.body());
		
		//undertow.stop();
	}

	public static void main(String[] args) throws Exception
	{
		new RestEasyModuleTest().configureServlets();
	}
}

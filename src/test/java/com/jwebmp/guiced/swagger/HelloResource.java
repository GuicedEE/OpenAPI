package com.guicedee.guiced.swagger;

import com.google.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("hello")
public class HelloResource
{
	private final Greeter greeter;

	@Inject
	public HelloResource(final Greeter greeter)
	{
		this.greeter = greeter;
	}

	@GET
	@Path("{name}")
	public String hello(@PathParam("name") final String name) {
		return greeter.greet(name);
	}
}

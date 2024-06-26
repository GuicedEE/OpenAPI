package com.jwebmp.guiced.openapi.tests;

import com.google.inject.Inject;
import jakarta.ws.rs.*;

@ApplicationPath("rest")
@Path("hello")
@Produces("application/json")
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

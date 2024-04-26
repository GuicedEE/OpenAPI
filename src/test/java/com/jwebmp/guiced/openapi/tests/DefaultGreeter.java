package com.jwebmp.guiced.openapi.tests;

public class DefaultGreeter implements Greeter
{
	public String greet(final String name)
	{
		return "Hello " + name;
	}
}

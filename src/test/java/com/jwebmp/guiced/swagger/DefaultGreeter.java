package com.guicedee.guiced.swagger;

public class DefaultGreeter implements Greeter
{
	public String greet(final String name)
	{
		return "Hello " + name;
	}
}

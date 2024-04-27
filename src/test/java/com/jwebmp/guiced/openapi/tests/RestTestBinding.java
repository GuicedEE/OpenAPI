package com.jwebmp.guiced.openapi.tests;

import com.google.inject.AbstractModule;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;

public class RestTestBinding
		extends AbstractModule
		implements IGuiceModule<RestTestBinding>
{

	@Override
	protected void configure()
	{
		bind(HelloResource.class);
		bind(Greeter.class).to(DefaultGreeter.class);
	}
}

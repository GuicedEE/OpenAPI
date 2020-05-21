package com.guicedee.guicedservlets.swagger.jsonoverrides;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.guicedee.guicedinjection.GuiceContext;

import static com.guicedee.guicedinjection.interfaces.ObjectBinderKeys.*;

public class Json
{

	private static ObjectMapper mapper;

	public static String pretty(Object o)
	{
		try
		{
			return pretty().writeValueAsString(o);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static ObjectWriter pretty()
	{
		return mapper().writer(new DefaultPrettyPrinter());
	}

	public static ObjectMapper mapper()
	{
		if (mapper == null)
		{
			mapper = ObjectMapperInstance;
		}
		return mapper;
	}

	public static void prettyPrint(Object o)
	{
		try
		{
			System.out.println(pretty().writeValueAsString(o)
			                           .replace("\r", ""));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

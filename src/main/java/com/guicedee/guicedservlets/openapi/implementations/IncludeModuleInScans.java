package com.guicedee.guicedservlets.openapi.implementations;

import com.guicedee.guicedinjection.interfaces.IGuiceScanModuleInclusions;

import java.util.Set;

public class IncludeModuleInScans implements IGuiceScanModuleInclusions<IncludeModuleInScans>
{
	@Override
	public Set<String> includeModules()
	{
		return Set.of();
	}
}

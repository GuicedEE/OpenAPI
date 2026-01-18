package com.guicedee.guicedservlets.openapi.implementations;

import com.guicedee.client.services.config.IGuiceScanModuleInclusions;

import java.util.Set;

/**
 * Opt-in inclusion hook for module scanning in Guice discovery.
 *
 * <p>This implementation returns an empty set, relying on default scanning
 * behavior while still registering the module with the SPI.</p>
 */
public class IncludeModuleInScans implements IGuiceScanModuleInclusions<IncludeModuleInScans>
{
	/**
	 * Returns the module names to include for scan-based discovery.
	 *
	 * @return an empty set to indicate no additional modules
	 */
	@Override
	public Set<String> includeModules()
	{
		return Set.of();
	}
}

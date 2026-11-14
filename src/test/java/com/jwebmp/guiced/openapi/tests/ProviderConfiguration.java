package com.jwebmp.guiced.openapi.tests;

import com.guicedee.guicedservlets.openapi.services.IGuicedSwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import java.util.function.UnaryOperator;

/** Test SPI, inactive unless a provider test installs a contribution. */
public class ProviderConfiguration implements IGuicedSwaggerConfiguration
{
    public static UnaryOperator<OpenAPIConfiguration> contribution = UnaryOperator.identity();

    @Override
    public OpenAPIConfiguration config(OpenAPIConfiguration configuration)
    {
        return contribution.apply(configuration);
    }
}

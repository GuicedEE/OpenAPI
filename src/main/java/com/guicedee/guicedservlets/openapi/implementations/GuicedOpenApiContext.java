package com.guicedee.guicedservlets.openapi.implementations;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.integration.GenericOpenApiContext;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.rest.Reader;

/**
 * Initializes the provider's single-use configuration without Swagger's JSON
 * round trips. Advanced configurations retain the upstream initialization path.
 * The generated model and both output codecs are still built during startup.
 */
final class GuicedOpenApiContext extends GenericOpenApiContext<GuicedOpenApiContext>
{
    private final boolean standardConfiguration;

    GuicedOpenApiContext(OpenAPIConfiguration configuration)
    {
        setOpenApiConfiguration(configuration);
        standardConfiguration = configuration.getClass() == SwaggerConfiguration.class
                && configuration.getOpenAPI() == null
                && configuration.getDefaultResponseCode() == null
                && configuration.getOpenAPIVersion() == null
                && configuration.getReaderClass() == null
                && configuration.getScannerClass() == null
                && configuration.getObjectMapperProcessorClass() == null
                && (configuration.getModelConverterClasses() == null || configuration.getModelConverterClasses().isEmpty())
                && !Boolean.TRUE.equals(configuration.isSortOutput())
                && !Boolean.TRUE.equals(configuration.isConvertToOpenAPI31())
                && (configuration.getCacheTTL() == null || configuration.getCacheTTL() < 0);
        setOpenApiReader(standardConfiguration ? new ConfigurationReader(configuration) : new Reader(configuration));
    }

    @Override
    public GuicedOpenApiContext init() throws OpenApiConfigurationException
    {
        OpenAPIConfiguration configuration = getOpenApiConfiguration();
        if (!standardConfiguration || getParent() != null)
        {
            // Conversion also needs 3.1 output codecs when the reader targets 3.0.
            if (Boolean.TRUE.equals(configuration.isConvertToOpenAPI31()))
            {
                setOutputJsonMapper(Json31.mapper());
                setOutputYamlMapper(Yaml31.mapper());
            }
            return super.init();
        }

        // There is no parent, config file to load, mapper processor, converter or
        // sorted-output builder here. GenericOpenApiContext.init() still clones
        // configuration twice in this case, including schema deserializer setup.
        // Initialize its public state directly and retain its read/cache behavior.
        boolean openAPI31 = Boolean.TRUE.equals(configuration.isOpenAPI31());
        setOpenAPI31(openAPI31);
        setConvertToOpenAPI31(false);
        setSchemaResolution(configuration.getSchemaResolution());
        if (configuration.getCacheTTL() != null) setCacheTTL(configuration.getCacheTTL());
        setOutputJsonMapper(openAPI31 ? Json31.mapper() : Json.mapper());
        setOutputYamlMapper(openAPI31 ? Yaml31.mapper() : Yaml.mapper());
        register();
        return this;
    }

    private static final class ConfigurationReader extends Reader
    {
        ConfigurationReader(OpenAPIConfiguration configuration)
        {
            // This configuration belongs to one eager provider. There is no
            // caller-owned seed model to clone, nor custom response/version
            // initialization to perform. Reader only reads these settings.
            super((OpenAPIConfiguration) null);
            this.config = configuration;
            if (Boolean.TRUE.equals(configuration.isOpenAPI31())) getOpenAPI().setOpenapi("3.1.0");
        }
    }
}

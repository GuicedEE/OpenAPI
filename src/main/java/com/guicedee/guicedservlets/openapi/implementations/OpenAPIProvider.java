package com.guicedee.guicedservlets.openapi.implementations;

import com.google.inject.Provider;
import com.guicedee.guicedservlets.openapi.services.IGuicedSwaggerConfiguration;
import io.swagger.v3.oas.integration.*;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiConfigurationLoader;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Guice provider that initializes and exposes a singleton {@link OpenAPI} model.
 *
 * <p>The provider builds a {@link SwaggerConfiguration}, applies any
 * {@link IGuicedSwaggerConfiguration} SPI contributions, configures the scanner
 * and reader, and registers a named context. The resulting model is read once
 * during construction and reused for subsequent injections.</p>
 */
public class OpenAPIProvider implements Provider<OpenAPI>
{
    /**
     * Current OpenAPI configuration, possibly updated by SPI contributors.
     */
    private OpenAPIConfiguration configuration;
    /**
     * Cached OpenAPI model read from the configured context.
     */
    private OpenAPI openAPI;

    /**
     * Known configuration loaders keyed by scheme name.
     */
    private static final Map<String, OpenApiConfigurationLoader> LOADERS = getLocationLoaders();
    /**
     * Default file names (classpath or file based) searched for configuration.
     */
    private static final List<ImmutablePair<String, String>> KNOWN_LOCATIONS = Arrays.asList(new ImmutablePair("classpath", "openapi-configuration.yaml"), new ImmutablePair("classpath", "openapi-configuration.json"), new ImmutablePair("classpath", "openapi.yaml"), new ImmutablePair("classpath", "openapi.json"), new ImmutablePair("file", "openapi-configuration.yaml"), new ImmutablePair("file", "openapi-configuration.json"), new ImmutablePair("file", "openapi.yaml"), new ImmutablePair("file", "openapi.json"));

    /**
     * Builds the default OpenAPI model and registers the context.
     *
     * <p>Constructs a {@link GenericOpenApiContext} with the id {@code context},
     * assigns a {@link GuicedOpenApiScanner}, and reads the OpenAPI model once.</p>
     *
     * @throws RuntimeException if the OpenAPI context cannot initialize
     */
    public OpenAPIProvider()
    {
        SwaggerConfiguration swaggerConfiguration = new SwaggerConfiguration();
        swaggerConfiguration.setReadAllResources(true);
        swaggerConfiguration.setConvertToOpenAPI31(true);
        swaggerConfiguration.setAlwaysResolveAppPath(true);
        swaggerConfiguration.setPrettyPrint(true);
        this.configuration = swaggerConfiguration;
        locateDefaultConfiguration();
        ServiceLoader<IGuicedSwaggerConfiguration> load = ServiceLoader.load(IGuicedSwaggerConfiguration.class);
        for (IGuicedSwaggerConfiguration conf : load)
        {
            configuration = conf.config(configuration);
        }
        GenericOpenApiContext cc = new GenericOpenApiContext();
        cc.setId("context");
        cc.convertToOpenAPI31(true);
        GuicedOpenApiScanner scanner = new GuicedOpenApiScanner();
        scanner.setConfiguration(configuration);
        cc.setOpenApiScanner(scanner);
        cc.setOpenApiReader(new io.swagger.v3.jaxrs2.Reader());

        try
        {
            cc.init();
        }
        catch (OpenApiConfigurationException e)
        {
            throw new RuntimeException(e);
        }
        OpenApiContextLocator.getInstance().putOpenApiContext("context",cc);
        openAPI = cc.read();
    }

    /**
     * Creates the loader registry for the supported configuration schemes.
     *
     * @return a map keyed by {@code classpath} or {@code file} scheme
     */
    private static Map<String, OpenApiConfigurationLoader> getLocationLoaders() {
        Map<String, OpenApiConfigurationLoader> map = new HashMap();
        map.put("classpath", new ClasspathOpenApiConfigurationLoader());
        map.put("file", new FileOpenApiConfigurationLoader());
        return map;
    }

    /**
     * Locates the first OpenAPI configuration file from the known defaults.
     *
     * @return an optional file name when a default configuration exists
     */
    public static Optional<String> locateDefaultConfiguration() {
        return KNOWN_LOCATIONS.stream().filter((location) -> {
            return ((OpenApiConfigurationLoader)LOADERS.get(location.left)).exists((String)location.right);
        }).findFirst().map(Pair::getValue);
    }

    /**
     * Returns the cached OpenAPI model produced at startup.
     *
     * @return the OpenAPI instance for this application
     */
    @Override
    public OpenAPI get()
    {
        return openAPI;
    }
}

package com.guicedee.guicedservlets.openapi.implementations;

import com.google.inject.Provider;
import com.guicedee.guicedservlets.openapi.services.IGuicedSwaggerConfiguration;
import io.swagger.v3.oas.integration.*;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiConfigurationLoader;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
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
    private final OpenAPIConfiguration configuration;
    /**
     * Cached OpenAPI model read from the configured context.
     */
    private final OpenAPI openAPI;

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
        OpenAPIConfiguration configured = loadDefaultConfiguration();
        ServiceLoader<IGuicedSwaggerConfiguration> load = ServiceLoader.load(IGuicedSwaggerConfiguration.class);
        for (IGuicedSwaggerConfiguration conf : load)
        {
            configured = Objects.requireNonNull(conf.config(configured),
                    "OpenAPI configuration SPI returned null: " + conf.getClass().getName());
        }
        configuration = configured;
        GenericOpenApiContext<?> cc = new GuicedOpenApiContext(configuration);
        cc.setId("context");
        GuicedOpenApiScanner scanner = new GuicedOpenApiScanner();
        scanner.setConfiguration(configuration);
        cc.setOpenApiScanner(scanner);

        try
        {
            cc.init();
        }
        catch (OpenApiConfigurationException e)
        {
            throw new RuntimeException(e);
        }
        openAPI = cc.read();
    }

    private static OpenAPIConfiguration loadDefaultConfiguration()
    {
        SwaggerConfiguration defaults = new SwaggerConfiguration().openAPI31(true);
        for (var location : KNOWN_LOCATIONS)
        {
            OpenApiConfigurationLoader loader = LOADERS.get(location.getLeft());
            if (loader.exists(location.getRight()))
            {
                try
                {
                    OpenAPIConfiguration loaded = loader.load(location.getRight());
                    if (!(loaded instanceof SwaggerConfiguration swagger))
                    {
                        throw new IllegalStateException("Invalid OpenAPI configuration: " + location);
                    }
                    defaults = swagger;
                    break;
                }
                catch (IOException e)
                {
                    throw new IllegalStateException("Cannot read OpenAPI configuration: " + location, e);
                }
            }
        }
        if (defaults.isReadAllResources() == null) defaults.setReadAllResources(true);
        if (defaults.isOpenAPI31() == null) defaults.setOpenAPI31(true);
        if (defaults.isConvertToOpenAPI31() == null) defaults.setConvertToOpenAPI31(false);
        if (defaults.isAlwaysResolveAppPath() == null) defaults.setAlwaysResolveAppPath(true);
        if (defaults.isPrettyPrint() == null) defaults.setPrettyPrint(true);
        return defaults;
    }

    /**
     * Creates the loader registry for the supported configuration schemes.
     *
     * @return a map keyed by {@code classpath} or {@code file} scheme
     */
    private static Map<String, OpenApiConfigurationLoader> getLocationLoaders() {
        Map<String, OpenApiConfigurationLoader> map = new HashMap<>();
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

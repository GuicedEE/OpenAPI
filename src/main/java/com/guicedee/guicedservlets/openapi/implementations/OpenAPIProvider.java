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

public class OpenAPIProvider implements Provider<OpenAPI>
{
    private OpenAPIConfiguration configuration;
    private OpenAPI openAPI;

    private static final Map<String, OpenApiConfigurationLoader> LOADERS = getLocationLoaders();
    private static final List<ImmutablePair<String, String>> KNOWN_LOCATIONS = Arrays.asList(new ImmutablePair("classpath", "openapi-configuration.yaml"), new ImmutablePair("classpath", "openapi-configuration.json"), new ImmutablePair("classpath", "openapi.yaml"), new ImmutablePair("classpath", "openapi.json"), new ImmutablePair("file", "openapi-configuration.yaml"), new ImmutablePair("file", "openapi-configuration.json"), new ImmutablePair("file", "openapi.yaml"), new ImmutablePair("file", "openapi.json"));

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

    private static Map<String, OpenApiConfigurationLoader> getLocationLoaders() {
        Map<String, OpenApiConfigurationLoader> map = new HashMap();
        map.put("classpath", new ClasspathOpenApiConfigurationLoader());
        map.put("file", new FileOpenApiConfigurationLoader());
        return map;
    }

    public static Optional<String> locateDefaultConfiguration() {
        return KNOWN_LOCATIONS.stream().filter((location) -> {
            return ((OpenApiConfigurationLoader)LOADERS.get(location.left)).exists((String)location.right);
        }).findFirst().map(Pair::getValue);
    }

    @Override
    public OpenAPI get()
    {
        return openAPI;
    }
}

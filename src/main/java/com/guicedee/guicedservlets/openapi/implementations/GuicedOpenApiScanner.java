package com.guicedee.guicedservlets.openapi.implementations;


import com.guicedee.client.IGuiceContext;
import io.github.classgraph.ScanResult;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Webhooks;
import io.swagger.v3.oas.integration.IgnoredPackages;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiScanner;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Log4j2
public class GuicedOpenApiScanner implements OpenApiScanner
{

    static final Set<String> ignored = new HashSet<>();

    static
    {
        ignored.addAll(IgnoredPackages.ignored);
        ignored.add("io.swagger.v3.jaxrs2");
        ignored.add("com.guicedee.guicedservlets.openapi.implementations");
    }

    OpenAPIConfiguration openApiConfiguration;

    @Override
    public void setConfiguration(OpenAPIConfiguration openApiConfiguration)
    {
        this.openApiConfiguration = openApiConfiguration;
    }

    @Override
    public Set<Class<?>> classes()
    {
        Set<String> acceptablePackages = new HashSet<>();

        Set<Class<?>> output = new HashSet<>();

        boolean allowAllPackages = false;

        // if classes are passed, use them
        if (openApiConfiguration.getResourceClasses() != null && !openApiConfiguration.getResourceClasses()
                                                                                      .isEmpty())
        {
            for (String className : openApiConfiguration.getResourceClasses())
            {
                if (!isIgnored(className))
                {
                    try
                    {
                        output.add(Class.forName(className));
                    }
                    catch (ClassNotFoundException e)
                    {
                        log.warn("error loading class from resourceClasses: " + e.getMessage(), e);
                    }
                }
            }
            return output;
        }

        if (openApiConfiguration.getResourcePackages() != null && !openApiConfiguration.getResourcePackages()
                                                                                       .isEmpty())
        {
            for (String pkg : openApiConfiguration.getResourcePackages())
            {
                if (!isIgnored(pkg))
                {
                    acceptablePackages.add(pkg);
                    //  graph.whitelistPackages(pkg);
                }
            }
        }
        else
        {
            allowAllPackages = true;
        }

        final Set<Class<?>> classes;
        ScanResult scanResult = IGuiceContext.instance()
                                             .getScanResult();
        classes = new HashSet<>(scanResult.getClassesWithAnnotation(OpenAPIDefinition.class.getName())
                                          .loadClasses());
        classes.addAll(new HashSet<>(scanResult.getClassesWithAnnotation(Webhooks.class.getName())
                                               .loadClasses()));
        classes.addAll(new HashSet<>(scanResult.getClassesWithAnnotation("jakarta.ws.rs.Path")
                                               .loadClasses()));
        classes.addAll(new HashSet<>(scanResult.getClassesWithAnnotation("jakarta.ws.rs.ApplicationPath")
                .loadClasses()));
        nextclass:
        for (Class<?> cls : classes)
        {
            if (allowAllPackages)
            {
                for (String s : ignored)
                {
                    if (cls.getPackage()
                           .getName()
                           .startsWith(s))
                    {
                        continue nextclass;
                    }
                }
                output.add(cls);
            }
            else
            {
                for (String pkg : acceptablePackages)
                {
                    if (cls.getPackage()
                           .getName()
                           .startsWith(pkg))
                    {
                        output.add(cls);
                    }
                }
            }
        }

        return output;
    }

    @Override
    public Map<String, Object> resources()
    {
        return new HashMap<>();
    }

    protected boolean isIgnored(String classOrPackageName)
    {
        if (StringUtils.isBlank(classOrPackageName))
        {
            return true;
        }
        return ignored.stream()
                      .anyMatch(classOrPackageName::startsWith);
    }

}

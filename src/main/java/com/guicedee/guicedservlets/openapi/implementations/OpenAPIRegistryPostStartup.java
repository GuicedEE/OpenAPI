package com.guicedee.guicedservlets.openapi.implementations;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import com.guicedee.client.Environment;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.lifecycle.IGuicePostStartup;
import com.guicedee.vertx.spi.VertXPreStartup;
import io.smallrye.mutiny.Uni;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.extern.log4j.Log4j2;

import java.net.URI;
import java.util.*;

/**
 * Post-startup hook that fetches OpenAPI specs from registered services
 * and merges them into the local OpenAPI model.
 * <p>
 * Features:
 * <ul>
 *   <li>Fetches remote OpenAPI specs from services with {@code openApiPath} configured</li>
 *   <li>Merges paths (prefixed with service name) and components (schemas prefixed)</li>
 *   <li>Adds OpenAPI {@code servers} entries from service URLs (internal, external, k8s)</li>
 *   <li>Respects {@code openApiEnvironments} — only merges for matching environment</li>
 *   <li>Environment detected from {@code ENVIRONMENT} / {@code ENV} / {@code APP_ENVIRONMENT} env vars</li>
 *   <li>No hard dependency on service-registry (uses reflection)</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>{@code
 * @RegisteredService(name = "payment-api",
 *     openApiPath = "/openapi.json",
 *     openApiEnvironments = {"dev", "int", "prod"})
 * @RegisteredService(name = "internal-only-api",
 *     openApiPath = "/openapi.json",
 *     openApiEnvironments = {"dev", "int"})  // not merged in prod
 * }</pre>
 */
@Log4j2
public class OpenAPIRegistryPostStartup implements IGuicePostStartup<OpenAPIRegistryPostStartup>
{
    @Override
    public Integer sortOrder()
    {
        return Integer.MIN_VALUE + 800; // After health checks have run (MIN+700)
    }

    @Override
    public List<Uni<Boolean>> postLoad()
    {
        try
        {
            Class<?> registryClass = Class.forName("com.guicedee.service.registry.ServiceRegistry");

            // Detect current environment
            String currentEnv = detectEnvironment();
            log.debug("OpenAPI registry merge — detected environment: '{}'", currentEnv);

            // Get all OpenAPI URLs filtered by environment
            var allOpenApiUrlsMethod = registryClass.getMethod("allOpenApiUrls", String.class);
            @SuppressWarnings("unchecked")
            Map<String, String> openApiUrls = (Map<String, String>) allOpenApiUrlsMethod.invoke(null, currentEnv);

            if (openApiUrls.isEmpty())
            {
                log.debug("No services with OpenAPI specs configured for environment '{}'", currentEnv);
                return List.of();
            }

            OpenAPI localOpenAPI = IGuiceContext.get(OpenAPI.class);
            if (localOpenAPI == null)
            {
                log.warn("Local OpenAPI model not available — skipping spec merge");
                return List.of();
            }

            // Add servers from service registry entries
            addServersFromRegistry(localOpenAPI, registryClass, currentEnv);

            // Reactive fetch-and-merge over the Vert.x event loop — never blocks the thread
            Vertx vertx = VertXPreStartup.getVertx();
            if (vertx == null)
            {
                log.warn("⚠️ Vertx not available — cannot fetch remote OpenAPI specs");
                return List.of();
            }

            // Use Swagger's own OpenAPI 3.1 mapper (a private copy so we don't mutate the
            // shared singleton). It registers the custom Schema deserializers that correctly
            // turn nodes like `additionalProperties` into Boolean/Schema instances. A plain
            // Jackson ObjectMapper lacks these and fails with
            // "additionalProperties must be either a Boolean or a Schema instance" on any
            // Map-typed property (e.g. Map<String,String>), which is valid OpenAPI.
            ObjectMapper mapper = Json31.mapper().rebuild()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .build();
            WebClient client = WebClient.create(vertx, new WebClientOptions()
                    .setConnectTimeout(5000)
                    .setTrustAll(true)
                    .setFollowRedirects(true));

            // The set of all services that participate in the registry merge. A remote spec
            // fetched from any of these may itself already contain peer paths/components that
            // were merged in (namespaced under "/{peer}" or "{peer}_"). We must NOT re-merge
            // those transitive entries, otherwise prefixes cascade
            // (e.g. /ne1-core/ne1-service-registry/ne1-service-registry/...). We only take each
            // service's OWN paths/components — its peers are picked up directly from each owner.
            Set<String> knownServiceNames = new HashSet<>(openApiUrls.keySet());

            List<Uni<Boolean>> fetches = new ArrayList<>(openApiUrls.size());
            for (var entry : openApiUrls.entrySet())
            {
                String serviceName = entry.getKey();
                String specUrl = entry.getValue();
                fetches.add(fetchAndMerge(localOpenAPI, client, mapper, serviceName, specUrl, knownServiceNames));
            }

            int count = openApiUrls.size();
            // Join all fetches into a single Uni so the post-startup pipeline awaits them
            // reactively, then close the WebClient and log once everything has settled.
            Uni<Boolean> merged = Uni.join().all(fetches).andCollectFailures()
                    .onItemOrFailure().invoke((results, err) -> {
                        client.close();
                        log.info("📖 OpenAPI registry merge complete — {} service specs processed", count);
                    })
                    .onItem().transform(results -> true)
                    .onFailure().recoverWithItem(true);

            return List.of(merged);
        }
        catch (ClassNotFoundException e)
        {
            log.debug("service-registry module not on classpath — skipping OpenAPI merge");
        }
        catch (Exception e)
        {
            log.warn("Error during OpenAPI registry merge: {}", e.getMessage());
        }

        return List.of();
    }

    /**
     * Adds OpenAPI Server entries from registered services.
     * Each service contributes servers for its internal URL, external URLs, and Kubernetes URL.
     */
    private void addServersFromRegistry(OpenAPI localOpenAPI, Class<?> registryClass, String currentEnv) throws Exception
    {
        var allMethod = registryClass.getMethod("all");
        @SuppressWarnings("unchecked")
        Map<String, Object> allServices = (Map<String, Object>) allMethod.invoke(null);

        if (localOpenAPI.getServers() == null)
        {
            localOpenAPI.setServers(new ArrayList<>());
        }

        Set<String> existingServerUrls = new HashSet<>();
        for (Server s : localOpenAPI.getServers())
        {
            if (s.getUrl() != null) existingServerUrls.add(s.getUrl());
        }

        for (var serviceEntry : allServices.entrySet())
        {
            Object entry = serviceEntry.getValue();
            String name = (String) entry.getClass().getMethod("name").invoke(entry);
            String url = (String) entry.getClass().getMethod("url").invoke(entry);

            // Check if this service has openApiPath
            @SuppressWarnings("unchecked")
            Map<String, String> metadata = (Map<String, String>) entry.getClass().getMethod("metadata").invoke(entry);
            if (!metadata.containsKey("openApiPath")) continue;

            // Check environment filter
            String envFilter = metadata.getOrDefault("openApiEnvironments", "");
            if (!envFilter.isEmpty() && currentEnv != null && !currentEnv.isEmpty())
            {
                boolean allowed = Arrays.stream(envFilter.split(","))
                        .map(String::trim)
                        .anyMatch(e -> e.equalsIgnoreCase(currentEnv));
                if (!allowed) continue;
            }

            // Add internal URL as server
            if (url != null && !url.isEmpty() && !existingServerUrls.contains(url))
            {
                Server server = new Server();
                server.setUrl(url);
                server.setDescription(name + " (internal)");
                localOpenAPI.getServers().add(server);
                existingServerUrls.add(url);
            }

            // Add external URLs as servers
            @SuppressWarnings("unchecked")
            List<String> externalUrls = (List<String>) entry.getClass().getMethod("externalUrls").invoke(entry);
            if (externalUrls != null)
            {
                for (String extUrl : externalUrls)
                {
                    if (!extUrl.isEmpty() && !existingServerUrls.contains(extUrl))
                    {
                        Server server = new Server();
                        server.setUrl(extUrl);
                        server.setDescription(name + " (external)");
                        localOpenAPI.getServers().add(server);
                        existingServerUrls.add(extUrl);
                    }
                }
            }

            // Add Kubernetes URL as server
            String k8sUrl = (String) entry.getClass().getMethod("kubernetesUrl").invoke(entry);
            if (k8sUrl != null && !k8sUrl.isEmpty() && !existingServerUrls.contains(k8sUrl))
            {
                Server server = new Server();
                server.setUrl(k8sUrl);
                server.setDescription(name + " (kubernetes)");
                localOpenAPI.getServers().add(server);
                existingServerUrls.add(k8sUrl);
            }

            // Auto-generate security schemes from authScheme metadata
            String authScheme = metadata.getOrDefault("authScheme", "");
            if (!authScheme.isEmpty())
            {
                addSecuritySchemeFromMetadata(localOpenAPI, name, authScheme, metadata);
            }
        }
    }

    /**
     * Auto-generates an OpenAPI SecurityScheme from service metadata.
     * Supports bearer, basic, apiKey, oauth2, and openIdConnect types.
     */
    private void addSecuritySchemeFromMetadata(OpenAPI openAPI, String serviceName, String authScheme, Map<String, String> metadata)
    {
        if (openAPI.getComponents() == null)
        {
            openAPI.setComponents(new Components());
        }
        if (openAPI.getComponents().getSecuritySchemes() == null)
        {
            openAPI.getComponents().setSecuritySchemes(new LinkedHashMap<>());
        }

        String schemeName = serviceName + "_auth";
        if (openAPI.getComponents().getSecuritySchemes().containsKey(schemeName))
        {
            return; // Already exists
        }

        SecurityScheme scheme = new SecurityScheme();
        switch (authScheme.toLowerCase())
        {
            case "bearer" -> {
                scheme.setType(SecurityScheme.Type.HTTP);
                scheme.setScheme("bearer");
                scheme.setBearerFormat("JWT");
                scheme.setDescription("Bearer token authentication for " + serviceName);
            }
            case "basic" -> {
                scheme.setType(SecurityScheme.Type.HTTP);
                scheme.setScheme("basic");
                scheme.setDescription("Basic authentication for " + serviceName);
            }
            case "apikey", "api_key" -> {
                scheme.setType(SecurityScheme.Type.APIKEY);
                scheme.setIn(SecurityScheme.In.HEADER);
                scheme.setName("X-API-Key");
                scheme.setDescription("API Key authentication for " + serviceName);
            }
            case "oauth2" -> {
                scheme.setType(SecurityScheme.Type.OAUTH2);
                scheme.setDescription("OAuth2 authentication for " + serviceName);
                OAuthFlows flows = new OAuthFlows();
                OAuthFlow clientCredentials = new OAuthFlow();
                String tokenUrl = metadata.getOrDefault("authTokenUrl", "");
                if (!tokenUrl.isEmpty())
                {
                    clientCredentials.setTokenUrl(tokenUrl);
                }
                Scopes scopes = new Scopes();
                String scopeStr = metadata.getOrDefault("authScopes", "");
                if (!scopeStr.isEmpty())
                {
                    for (String scopePair : scopeStr.split("\\|"))
                    {
                        String[] parts = scopePair.split("=", 2);
                        if (parts.length == 2)
                        {
                            scopes.addString(parts[0].trim(), parts[1].trim());
                        }
                        else
                        {
                            scopes.addString(parts[0].trim(), "");
                        }
                    }
                }
                clientCredentials.setScopes(scopes);
                flows.setClientCredentials(clientCredentials);
                scheme.setFlows(flows);
            }
            case "openidconnect", "oidc" -> {
                scheme.setType(SecurityScheme.Type.OPENIDCONNECT);
                String tokenUrl = metadata.getOrDefault("authTokenUrl", "");
                if (!tokenUrl.isEmpty())
                {
                    scheme.setOpenIdConnectUrl(tokenUrl);
                }
                scheme.setDescription("OpenID Connect authentication for " + serviceName);
            }
            default -> {
                log.warn("⚠️ Unknown authScheme '{}' for service '{}' — skipping", authScheme, serviceName);
                return;
            }
        }

        openAPI.getComponents().getSecuritySchemes().put(schemeName, scheme);

        // Add global security requirement for this service
        if (openAPI.getSecurity() == null)
        {
            openAPI.setSecurity(new ArrayList<>());
        }
        SecurityRequirement requirement = new SecurityRequirement();
        requirement.addList(schemeName);
        openAPI.getSecurity().add(requirement);

        log.debug("🔐 Auto-generated '{}' security scheme for service '{}'", authScheme, serviceName);
    }

    public Uni<Boolean> fetchAndMerge(OpenAPI localOpenAPI, WebClient client, ObjectMapper mapper,
                                      String serviceName, String specUrl)
    {
        return fetchAndMerge(localOpenAPI, client, mapper, serviceName, specUrl, Set.of());
    }

    public Uni<Boolean> fetchAndMerge(OpenAPI localOpenAPI, WebClient client, ObjectMapper mapper,
                                      String serviceName, String specUrl, Set<String> knownServiceNames)
    {
        try
        {
            URI uri = URI.create(specUrl);
            int port = uri.getPort() > 0 ? uri.getPort()
                    : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
            boolean ssl = "https".equalsIgnoreCase(uri.getScheme());
            String path = (uri.getRawPath() == null || uri.getRawPath().isEmpty()) ? "/" : uri.getRawPath();
            if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty())
            {
                path = path + "?" + uri.getRawQuery();
            }
            final String requestUri = path;

            return Uni.createFrom().completionStage(() ->
                            client.get(port, uri.getHost(), requestUri)
                                    .ssl(ssl)
                                    .putHeader("Accept", "application/json, text/json, */*")
                                    .timeout(10_000)
                                    .send()
                                    .toCompletionStage())
                    .onItem().transform(response -> {
                        if (response.statusCode() == 200)
                        {
                            try
                            {
                                OpenAPI remoteSpec = mapper.readValue(response.bodyAsString(), OpenAPI.class);
                                mergeSpec(localOpenAPI, remoteSpec, serviceName, knownServiceNames);
                                log.info("✅ Merged OpenAPI spec from '{}' ({} paths)", serviceName,
                                        remoteSpec.getPaths() != null ? remoteSpec.getPaths().size() : 0);
                            }
                            catch (Exception e)
                            {
                                log.warn("⚠️ Error parsing OpenAPI spec from '{}' ({}): {}",
                                        serviceName, specUrl, e.getMessage());
                            }
                        }
                        else
                        {
                            log.warn("⚠️ Failed to fetch OpenAPI spec from '{}' ({}): HTTP {}",
                                    serviceName, specUrl, response.statusCode());
                        }
                        return true;
                    })
                    .onFailure().recoverWithItem(err -> {
                        log.warn("⚠️ Error fetching OpenAPI spec from '{}' ({}): {}",
                                serviceName, specUrl, err.getMessage());
                        return true;
                    });
        }
        catch (Exception e)
        {
            log.warn("⚠️ Error fetching OpenAPI spec from '{}' ({}): {}",
                    serviceName, specUrl, e.getMessage());
            return Uni.createFrom().item(true);
        }
    }

    /**
     * Merges paths, components, servers, tags, and security from a remote OpenAPI spec into the local one.
     * <p>
     * Only the remote service's <b>own</b> paths/components are merged. Entries that are already
     * namespaced under a known registry service (path segment {@code /{peer}/...} or component
     * key {@code {peer}_...}) are <b>skipped</b> — those are transitive merges the remote performed
     * for its peers, and re-prefixing them here would cascade prefixes
     * (e.g. {@code /a/b/b/...}) and produce duplicate operations. Each peer is merged directly
     * from its own spec instead.
     * <p>
     * Remote (own) paths are prefixed with {@code /{serviceName}} to avoid collisions, and the
     * prefix is applied idempotently. Schemas and security schemes are prefixed with
     * {@code {serviceName}_} to namespace them.
     *
     * @param knownServiceNames names of all services participating in the registry merge; used to
     *                          recognise and skip transitively-merged peer entries
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mergeSpec(OpenAPI local, OpenAPI remote, String serviceName, Set<String> knownServiceNames)
    {
        // ──── Merge Paths ────
        if (remote.getPaths() != null && !remote.getPaths().isEmpty())
        {
            if (local.getPaths() == null)
            {
                local.setPaths(new Paths());
            }

            String prefix = "/" + serviceName;
            for (Map.Entry<String, PathItem> pathEntry : remote.getPaths().entrySet())
            {
                String remotePath = pathEntry.getKey();

                // Skip paths the remote merged in from its own peers — they are already
                // namespaced under another registry service and would cascade if re-prefixed.
                if (isNamespacedByKnownService(remotePath, knownServiceNames, serviceName))
                {
                    continue;
                }

                String normalised = remotePath.startsWith("/") ? remotePath : "/" + remotePath;
                // Idempotent prefixing — never double up if already under this service's prefix.
                String mergedPath = (normalised.equals(prefix) || normalised.startsWith(prefix + "/"))
                        ? normalised
                        : prefix + normalised;
                local.getPaths().addPathItem(mergedPath, pathEntry.getValue());
            }
        }

        // ──── Merge Components ────
        if (remote.getComponents() != null)
        {
            if (local.getComponents() == null)
            {
                local.setComponents(new Components());
            }

            Components remoteComponents = remote.getComponents();
            Components localComponents = local.getComponents();

            // Schemas
            Map<String, Object> remoteSchemas = (Map) remoteComponents.getSchemas();
            if (remoteSchemas != null)
            {
                if (localComponents.getSchemas() == null)
                {
                    localComponents.setSchemas(new LinkedHashMap<>());
                }
                Map<String, Object> targetSchemas = (Map) localComponents.getSchemas();
                for (var schema : remoteSchemas.entrySet())
                {
                    // Skip schemas the remote merged from its peers (already "{peer}_X").
                    if (isNamespacedComponent(schema.getKey(), knownServiceNames, serviceName)) continue;
                    targetSchemas.putIfAbsent(serviceName + "_" + schema.getKey(), schema.getValue());
                }
            }

            // Security Schemes
            if (remoteComponents.getSecuritySchemes() != null)
            {
                if (localComponents.getSecuritySchemes() == null)
                {
                    localComponents.setSecuritySchemes(new LinkedHashMap<>());
                }
                for (var sec : remoteComponents.getSecuritySchemes().entrySet())
                {
                    if (isNamespacedComponent(sec.getKey(), knownServiceNames, serviceName)) continue;
                    localComponents.getSecuritySchemes()
                            .putIfAbsent(serviceName + "_" + sec.getKey(), sec.getValue());
                }
            }

            // Parameters
            Map<String, Object> remoteParams = (Map) remoteComponents.getParameters();
            if (remoteParams != null)
            {
                if (localComponents.getParameters() == null)
                {
                    localComponents.setParameters(new LinkedHashMap<>());
                }
                Map<String, Object> targetParams = (Map) localComponents.getParameters();
                for (var param : remoteParams.entrySet())
                {
                    targetParams.putIfAbsent(serviceName + "_" + param.getKey(), param.getValue());
                }
            }

            // Request Bodies
            Map<String, Object> remoteReqBodies = (Map) remoteComponents.getRequestBodies();
            if (remoteReqBodies != null)
            {
                if (localComponents.getRequestBodies() == null)
                {
                    localComponents.setRequestBodies(new LinkedHashMap<>());
                }
                Map<String, Object> targetReqBodies = (Map) localComponents.getRequestBodies();
                for (var rb : remoteReqBodies.entrySet())
                {
                    targetReqBodies.putIfAbsent(serviceName + "_" + rb.getKey(), rb.getValue());
                }
            }

            // Responses
            Map<String, Object> remoteResponses = (Map) remoteComponents.getResponses();
            if (remoteResponses != null)
            {
                if (localComponents.getResponses() == null)
                {
                    localComponents.setResponses(new LinkedHashMap<>());
                }
                Map<String, Object> targetResponses = (Map) localComponents.getResponses();
                for (var resp : remoteResponses.entrySet())
                {
                    targetResponses.putIfAbsent(serviceName + "_" + resp.getKey(), resp.getValue());
                }
            }

            // Headers
            Map<String, Object> remoteHeaders = (Map) remoteComponents.getHeaders();
            if (remoteHeaders != null)
            {
                if (localComponents.getHeaders() == null)
                {
                    localComponents.setHeaders(new LinkedHashMap<>());
                }
                Map<String, Object> targetHeaders = (Map) localComponents.getHeaders();
                for (var hdr : remoteHeaders.entrySet())
                {
                    targetHeaders.putIfAbsent(serviceName + "_" + hdr.getKey(), hdr.getValue());
                }
            }

            // Examples
            Map<String, Object> remoteExamples = (Map) remoteComponents.getExamples();
            if (remoteExamples != null)
            {
                if (localComponents.getExamples() == null)
                {
                    localComponents.setExamples(new LinkedHashMap<>());
                }
                Map<String, Object> targetExamples = (Map) localComponents.getExamples();
                for (var ex : remoteExamples.entrySet())
                {
                    targetExamples.putIfAbsent(serviceName + "_" + ex.getKey(), ex.getValue());
                }
            }

            // Links
            Map<String, Object> remoteLinks = (Map) remoteComponents.getLinks();
            if (remoteLinks != null)
            {
                if (localComponents.getLinks() == null)
                {
                    localComponents.setLinks(new LinkedHashMap<>());
                }
                Map<String, Object> targetLinks = (Map) localComponents.getLinks();
                for (var link : remoteLinks.entrySet())
                {
                    targetLinks.putIfAbsent(serviceName + "_" + link.getKey(), link.getValue());
                }
            }

            // Callbacks
            Map<String, Object> remoteCallbacks = (Map) remoteComponents.getCallbacks();
            if (remoteCallbacks != null)
            {
                if (localComponents.getCallbacks() == null)
                {
                    localComponents.setCallbacks(new LinkedHashMap<>());
                }
                Map<String, Object> targetCallbacks = (Map) localComponents.getCallbacks();
                for (var cb : remoteCallbacks.entrySet())
                {
                    targetCallbacks.putIfAbsent(serviceName + "_" + cb.getKey(), cb.getValue());
                }
            }
        }

        // ──── Merge Tags ────
        if (remote.getTags() != null && !remote.getTags().isEmpty())
        {
            if (local.getTags() == null)
            {
                local.setTags(new ArrayList<>());
            }
            Set<String> existingTags = new HashSet<>();
            for (var tag : local.getTags())
            {
                existingTags.add(tag.getName());
            }
            for (var remoteTag : remote.getTags())
            {
                if (!existingTags.contains(remoteTag.getName()))
                {
                    local.getTags().add(remoteTag);
                    existingTags.add(remoteTag.getName());
                }
            }
        }

        // ──── Merge Security Requirements ────
        if (remote.getSecurity() != null && !remote.getSecurity().isEmpty())
        {
            if (local.getSecurity() == null)
            {
                local.setSecurity(new ArrayList<>());
            }
            // Remap security requirement names to prefixed scheme names
            for (var remoteReq : remote.getSecurity())
            {
                var prefixedReq = new io.swagger.v3.oas.models.security.SecurityRequirement();
                for (var entry : remoteReq.entrySet())
                {
                    prefixedReq.addList(serviceName + "_" + entry.getKey(), entry.getValue());
                }
                local.getSecurity().add(prefixedReq);
            }
        }

        // ──── Merge Servers from remote spec ────
        if (remote.getServers() != null)
        {
            if (local.getServers() == null)
            {
                local.setServers(new ArrayList<>());
            }
            Set<String> existingUrls = new HashSet<>();
            for (Server s : local.getServers())
            {
                if (s.getUrl() != null) existingUrls.add(s.getUrl());
            }
            for (Server remoteServer : remote.getServers())
            {
                if (remoteServer.getUrl() != null && !existingUrls.contains(remoteServer.getUrl()))
                {
                    Server server = new Server();
                    server.setUrl(remoteServer.getUrl());
                    server.setDescription(serviceName + ": " +
                            (remoteServer.getDescription() != null ? remoteServer.getDescription() : remoteServer.getUrl()));
                    // Merge server variables if present
                    if (remoteServer.getVariables() != null)
                    {
                        server.setVariables(remoteServer.getVariables());
                    }
                    local.getServers().add(server);
                    existingUrls.add(remoteServer.getUrl());
                }
            }
        }
    }

    /**
     * Returns {@code true} when a remote path's first segment matches a known registry service
     * name — meaning the remote already merged this path from one of its peers. Such transitively
     * merged paths are skipped so prefixes don't cascade and operations aren't duplicated.
     * A path under the service's own prefix is <i>not</i> treated as foreign (handled idempotently).
     */
    private boolean isNamespacedByKnownService(String remotePath, Set<String> knownServiceNames, String serviceName)
    {
        if (remotePath == null || knownServiceNames == null || knownServiceNames.isEmpty()) return false;
        String p = remotePath.startsWith("/") ? remotePath.substring(1) : remotePath;
        int slash = p.indexOf('/');
        String firstSegment = slash >= 0 ? p.substring(0, slash) : p;
        if (firstSegment.isEmpty() || firstSegment.equals(serviceName)) return false;
        return knownServiceNames.contains(firstSegment);
    }

    /**
     * Returns {@code true} when a component key is already namespaced under a known registry
     * service (i.e. starts with {@code "{peer}_"}), indicating the remote merged it from a peer.
     */
    private boolean isNamespacedComponent(String key, Set<String> knownServiceNames, String serviceName)
    {
        if (key == null || knownServiceNames == null || knownServiceNames.isEmpty()) return false;
        int underscore = key.indexOf('_');
        if (underscore <= 0) return false;
        String firstSegment = key.substring(0, underscore);
        if (firstSegment.equals(serviceName)) return false;
        return knownServiceNames.contains(firstSegment);
    }

    /**
     * Detects the current environment from standard env vars.
     */
    private String detectEnvironment()
    {
        String env = Environment.getSystemPropertyOrEnvironment("ENVIRONMENT", "");
        if (!env.isEmpty()) return env;
        env = Environment.getSystemPropertyOrEnvironment("ENV", "");
        if (!env.isEmpty()) return env;
        env = Environment.getSystemPropertyOrEnvironment("APP_ENVIRONMENT", "");
        if (!env.isEmpty()) return env;
        env = Environment.getSystemPropertyOrEnvironment("SPRING_PROFILES_ACTIVE", "");
        if (!env.isEmpty()) return env;
        return "";
    }
}


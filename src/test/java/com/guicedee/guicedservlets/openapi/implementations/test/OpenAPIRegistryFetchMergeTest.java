package com.guicedee.guicedservlets.openapi.implementations.test;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import com.guicedee.guicedservlets.openapi.implementations.OpenAPIRegistryPostStartup;
import io.smallrye.mutiny.Uni;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link OpenAPIRegistryPostStartup#fetchAndMerge} uses the reactive
 * Vert.x {@link WebClient} pipeline — fetching a remote spec without blocking the
 * Vert.x event loop and merging paths/components into the local model.
 */
public class OpenAPIRegistryFetchMergeTest
{
    private static final String REMOTE_SPEC = """
            {
              "openapi": "3.0.1",
              "info": {"title": "Remote Service", "version": "1.0.0"},
              "paths": {
                "/ping": {"get": {"responses": {"200": {"description": "ok"}}}}
              },
              "components": {"schemas": {"Pong": {"type": "object"}}}
            }
            """;

    @Test
    void fetchAndMergeIsReactiveAndMerges() throws Exception
    {
        Vertx vertx = Vertx.vertx();
        try
        {
            // Tiny local server that serves a remote OpenAPI spec
            HttpServer server = vertx.createHttpServer()
                    .requestHandler(req -> req.response()
                            .putHeader("content-type", "application/json")
                            .end(REMOTE_SPEC))
                    .listen(0)
                    .toCompletionStage().toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);
            int port = server.actualPort();

            WebClient client = WebClient.create(vertx);
            ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .build();

            OpenAPI local = new OpenAPI();
            OpenAPIRegistryPostStartup startup = new OpenAPIRegistryPostStartup();

            Uni<Boolean> uni = startup.fetchAndMerge(local, client, mapper,
                    "remote", "http://localhost:" + port + "/openapi.json");

            // Reactive — completes off the calling thread; await result in the test thread
            Boolean result = uni.await().atMost(Duration.ofSeconds(10));
            assertTrue(result, "fetchAndMerge should complete with true");

            // Paths merged and prefixed with the service name
            assertNotNull(local.getPaths(), "local paths should be populated");
            assertTrue(local.getPaths().containsKey("/remote/ping"),
                    "remote path should be prefixed with the service name");

            // Schemas merged and namespaced
            assertNotNull(local.getComponents(), "local components should be populated");
            assertNotNull(local.getComponents().getSchemas(), "local schemas should be populated");
            assertTrue(local.getComponents().getSchemas().containsKey("remote_Pong"),
                    "remote schema should be namespaced with the service name");

            client.close();
            server.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
        finally
        {
            vertx.close();
        }
    }

    /**
     * A peer service's published spec already contains paths/schemas it merged from <i>its</i>
     * peers (namespaced under "/{peer}" and "{peer}_"). When the aggregator fetches that spec it
     * must take only the service's OWN entries and skip the transitively-merged ones — otherwise
     * prefixes cascade (e.g. /a/b/b/...) and operations duplicate.
     */
    private static final String AGGREGATED_PEER_SPEC = """
            {
              "openapi": "3.0.1",
              "info": {"title": "ne1-core", "version": "1.0.0"},
              "paths": {
                "/core/info": {"get": {"responses": {"200": {"description": "ok"}}}},
                "/ne1-service-registry/registry/{name}/url": {"get": {"responses": {"200": {"description": "ok"}}}}
              },
              "components": {"schemas": {
                "CoreInfo": {"type": "object"},
                "ne1-service-registry_RegistryEntry": {"type": "object"}
              }}
            }
            """;

    @Test
    void fetchAndMergeSkipsTransitivelyMergedPeerEntries() throws Exception
    {
        Vertx vertx = Vertx.vertx();
        try
        {
            HttpServer server = vertx.createHttpServer()
                    .requestHandler(req -> req.response()
                            .putHeader("content-type", "application/json")
                            .end(AGGREGATED_PEER_SPEC))
                    .listen(0)
                    .toCompletionStage().toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);
            int port = server.actualPort();

            WebClient client = WebClient.create(vertx);
            ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .build();

            OpenAPI local = new OpenAPI();
            OpenAPIRegistryPostStartup startup = new OpenAPIRegistryPostStartup();

            // Both services participate in the registry merge.
            java.util.Set<String> known = java.util.Set.of("ne1-core", "ne1-service-registry");

            Uni<Boolean> uni = startup.fetchAndMerge(local, client, mapper,
                    "ne1-core", "http://localhost:" + port + "/openapi.json", known);
            assertTrue(uni.await().atMost(Duration.ofSeconds(10)));

            assertNotNull(local.getPaths());
            // Own path is prefixed once.
            assertTrue(local.getPaths().containsKey("/ne1-core/core/info"),
                    "ne1-core's own path should be prefixed once");
            // Transitively-merged peer path must NOT cascade.
            assertFalse(local.getPaths().containsKey("/ne1-core/ne1-service-registry/registry/{name}/url"),
                    "transitively-merged peer path must be skipped (no cascading prefix)");

            // Own schema namespaced once; peer schema not re-namespaced.
            assertNotNull(local.getComponents());
            assertTrue(local.getComponents().getSchemas().containsKey("ne1-core_CoreInfo"));
            assertFalse(local.getComponents().getSchemas().containsKey("ne1-core_ne1-service-registry_RegistryEntry"),
                    "transitively-merged peer schema must be skipped");

            client.close();
            server.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
        finally
        {
            vertx.close();
        }
    }

    @Test
    void fetchAndMergeRecoversFromUnreachableServiceWithoutBlocking()
    {
        Vertx vertx = Vertx.vertx();
        try
        {
            WebClient client = WebClient.create(vertx);
            ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .build();

            OpenAPI local = new OpenAPI();
            OpenAPIRegistryPostStartup startup = new OpenAPIRegistryPostStartup();

            // Port 1 is unreachable — must not throw, must recover reactively with true
            Uni<Boolean> uni = startup.fetchAndMerge(local, client, mapper,
                    "down-service", "http://localhost:1/openapi.json");

            Boolean result = uni.await().atMost(Duration.ofSeconds(15));
            assertTrue(result, "fetchAndMerge should recover from failure with true");
            assertFalse(local.getPaths() != null && local.getPaths().containsKey("/down-service/ping"),
                    "no paths should be merged from an unreachable service");

            client.close();
        }
        finally
        {
            vertx.close();
        }
    }
}


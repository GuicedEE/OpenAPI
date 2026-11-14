package com.jwebmp.guiced.openapi.tests;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import java.util.Map;

@Path("/provider")
@Produces("application/json")
public class ProviderResource
{
    @GET
    @Operation(summary = "Typed map")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MapResponse.class)))
    public MapResponse get() { throw new AssertionError("Spec generation must not invoke endpoints"); }

    @GET
    @Path("/unannotated")
    public String unannotated() { throw new AssertionError("Spec generation must not invoke endpoints"); }

    @Schema(name = "ProviderMapResponse")
    public static class MapResponse { public Map<String, MapValue> entries; }

    @Schema(name = "ProviderMapValue")
    public static class MapValue { public String value; }
}

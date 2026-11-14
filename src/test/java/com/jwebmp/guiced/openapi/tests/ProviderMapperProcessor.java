package com.jwebmp.guiced.openapi.tests;

import io.swagger.v3.oas.integration.api.ObjectMapperProcessor;
import tools.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;

public class ProviderMapperProcessor implements ObjectMapperProcessor
{
    public static final Set<String> calls = new HashSet<>();
    public ObjectMapper processJsonObjectMapper(ObjectMapper mapper) { calls.add("model"); return mapper; }
    public ObjectMapper processOutputJsonObjectMapper(ObjectMapper mapper) { calls.add("json"); return mapper; }
    public ObjectMapper processOutputYamlObjectMapper(ObjectMapper mapper) { calls.add("yaml"); return mapper; }
}

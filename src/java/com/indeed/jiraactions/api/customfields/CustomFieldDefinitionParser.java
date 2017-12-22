package com.indeed.jiraactions.api.customfields;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class CustomFieldDefinitionParser {
    private CustomFieldDefinitionParser() { /* No */ }

    private final static ObjectMapper mapper = new ObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    public static CustomFieldDefinition[] parseCustomFields(final InputStream in) throws IOException {
        return mapper.readValue(in, CustomFieldDefinition[].class);
    }
}

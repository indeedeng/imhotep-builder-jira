package com.indeed.jiraactions.api.customfields;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.indeed.jiraactions.api.CustomFieldDefinition;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

public abstract class CustomFieldParser {
    private CustomFieldParser() { /* No */ }
    private static final Logger log = Logger.getLogger(CustomFieldParser.class);
    private final static ObjectMapper mapper = new ObjectMapper()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

    public static CustomFieldDefinition[] parseCustomFields(final String filename) throws IOException {
        return mapper.readValue(filename, CustomFieldDefinition[].class);
    }

    @VisibleForTesting
    static CustomFieldDefinition[] parseCustomFields(final InputStream in) throws IOException {
        return mapper.readValue(in, CustomFieldDefinition[].class);
    }
}

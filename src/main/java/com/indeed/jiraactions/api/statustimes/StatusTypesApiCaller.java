package com.indeed.jiraactions.api.statustimes;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.indeed.jiraactions.JiraActionsIndexBuilderConfig;
import com.indeed.jiraactions.api.ApiCaller;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author mmurtiono
 */

public class StatusTypesApiCaller {
    private static final String API_PATH = "/rest/api/2/status";

    private final JiraActionsIndexBuilderConfig config;
    private final ApiCaller apiCaller;

    public StatusTypesApiCaller(final JiraActionsIndexBuilderConfig config, final ApiCaller apiCaller) {
        this.config = config;
        this.apiCaller = apiCaller;
    }

    public List<String> getStatusTypes() throws IOException {
        final ImmutableList.Builder<String> output = ImmutableList.builder();

        final JsonNode root = apiCaller.getJsonNode(config.getJiraBaseURL() + API_PATH);
        for (final Iterator<JsonNode> it = root.elements(); it.hasNext(); ) {
            final JsonNode node = it.next();
            output.add(node.get("name").textValue());
        }

        return output.build();
    }
}

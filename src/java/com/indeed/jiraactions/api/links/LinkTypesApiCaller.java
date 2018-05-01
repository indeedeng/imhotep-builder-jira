package com.indeed.jiraactions.api.links;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.indeed.jiraactions.JiraActionsIndexBuilderConfig;
import com.indeed.jiraactions.api.ApiCaller;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Each JIRA could have its own set of links. We need to know what links exist when we start so we can keep them
 * in the right place in the TSV.
 */
public class LinkTypesApiCaller {
    private static final String API_PATH = "/rest/api/2/issueLinkType";

    private final JiraActionsIndexBuilderConfig config;
    private final ApiCaller apiCaller;

    public LinkTypesApiCaller(final JiraActionsIndexBuilderConfig config, final ApiCaller apiCaller) {
        this.config = config;
        this.apiCaller = apiCaller;
    }

    public List<String> getLinkTypes() throws IOException {
        final ImmutableList.Builder<String> output = ImmutableList.builder();

        final JsonNode root = apiCaller.getJsonNode(config.getJiraBaseURL() + API_PATH);
        for (final Iterator<JsonNode> it = root.get("issueLinkTypes").elements(); it.hasNext(); ) {
            final JsonNode node = it.next();
            output.add(node.get("inward").textValue());
            output.add(node.get("outward").textValue());
        }

        return output.build();
    }
}

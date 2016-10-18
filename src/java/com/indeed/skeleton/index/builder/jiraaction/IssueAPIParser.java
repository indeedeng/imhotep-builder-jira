package com.indeed.skeleton.index.builder.jiraaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;

import java.io.IOException;

/**
 * Created by soono on 8/30/16.
 */
public class IssueAPIParser {
    private final static ObjectMapper mapper = new ObjectMapper();

    public static Issue getObject(final JsonNode issueNode) {
        Issue issue = new Issue();
        try {
            issue = mapper.treeToValue(issueNode, Issue.class);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return issue;
    }
}

package com.indeed.jiraactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.jiraactions.api.response.issue.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author soono on 8/30/16.
 */
public class IssueAPIParser {
    private static final Logger log = LoggerFactory.getLogger(IssueAPIParser.class);
    private final static ObjectMapper mapper = new ObjectMapper();

    @Nullable
    public static Issue getObject(final JsonNode issueNode) {
        final Issue issue;
        try {
            issue = mapper.treeToValue(issueNode, Issue.class);
            if(issue.fields.created == null) {
                log.warn("Invalid issue {} with no date.", issue.key);
                return null;
            }
        } catch (final IOException e) {
            log.error("Caught an error trying to parse a JSON node", e);
            return null;
        }
        return issue;
    }
}

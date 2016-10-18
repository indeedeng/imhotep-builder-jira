package com.indeed.skeleton.index.builder.jiraaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;
import com.indeed.util.logging.Loggers;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Created by soono on 8/30/16.
 */
public class IssueAPIParser {
    private static final Logger log = Logger.getLogger(IssueAPIParser.class);
    private final static ObjectMapper mapper = new ObjectMapper();

    @Nullable
    public static Issue getObject(final JsonNode issueNode) {
        Issue issue = new Issue();
        try {
            issue = mapper.treeToValue(issueNode, Issue.class);
            if(issue.fields.created == null) {
                Loggers.error(log, "Invalid issue %s with no date.", issue.key);
                return null;
            }
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
        return issue;
    }
}

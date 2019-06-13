package com.indeed.jiraactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.indeed.jiraactions.api.response.issue.Issue;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public interface PageProvider {
    boolean hasPage();

    JsonNode getRawPage() throws InterruptedException;

    void reset();

    @Nullable
    Issue processNode(final JsonNode issueNode);

    Iterable<Issue> getPage() throws InterruptedException;

    Action getAction(final Issue issue) throws IOException;

    void writeActions(final List<Action> actions) throws IOException;
}

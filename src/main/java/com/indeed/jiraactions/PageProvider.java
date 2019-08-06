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

    List<Action> getActions(final Issue issue) throws IOException;

    Action getJiraissues(final Action action,final Issue issue) throws IOException;

    void writeActions(final List<Action> actions) throws IOException;

    void writeIssue(final Action action) throws IOException;
}

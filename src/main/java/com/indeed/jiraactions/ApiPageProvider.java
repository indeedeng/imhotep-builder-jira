package com.indeed.jiraactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Stopwatch;
import com.indeed.jiraactions.api.IssueAPIParser;
import com.indeed.jiraactions.api.IssuesAPICaller;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.response.issue.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

public class ApiPageProvider implements PageProvider {
    private static final Logger log = LoggerFactory.getLogger(ApiPageProvider.class);

    private final IssuesAPICaller issuesAPICaller;
    private final ActionFactory actionFactory;
    private final TsvFileWriter tsvFileWriter;

    private final DateTime startDate;
    @SuppressWarnings("FieldCanBeLocal")

    private final DateTime endDate;
    private final Set<CustomFieldDefinition> customFieldsSeen;

    private long apiTime = 0;
    private long processTime = 0;
    private long fileTime = 0;

    public ApiPageProvider(final IssuesAPICaller issuesAPICaller, final ActionFactory actionFactory,
                           final JiraActionsIndexBuilderConfig config, final TsvFileWriter tsvFileWriter) {
        this.issuesAPICaller = issuesAPICaller;
        this.actionFactory = actionFactory;
        this.tsvFileWriter = tsvFileWriter;

        this.startDate = JiraActionsUtil.parseDateTime(config.getStartDate());
        this.endDate = JiraActionsUtil.parseDateTime(config.getEndDate());
        this.customFieldsSeen = new HashSet<>(config.getCustomFields().length);
    }

    public long getApiTime() {
        return apiTime;
    }

    public long getProcessingTime() {
        return processTime;
    }

    public long getFileWritingTime() {
        return fileTime;
    }

    public Set<CustomFieldDefinition> getCustomFieldsSeen() {
        return customFieldsSeen;
    }

    @Override
    public boolean hasPage() {
        return issuesAPICaller.currentPageExist();
    }

    @Override
    public JsonNode getRawPage() throws InterruptedException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final JsonNode issuesNode = issuesAPICaller.getIssuesNodeWithBackoff();
        stopwatch.stop();

        apiTime += stopwatch.elapsed(TimeUnit.MILLISECONDS);
        log.trace("{} ms for an API call.", stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return issuesNode;
    }

    @Override
    public Iterable<Issue> getPage() throws InterruptedException {
        final JsonNode rawPage = getRawPage();
        final Iterator<Issue> iterator = StreamSupport.stream(rawPage.spliterator(), false)
                .map(this::processNode)
                .filter(Objects::nonNull)
                .iterator();
        final Iterable<Issue> iterable = () -> iterator;
        return iterable;
    }

    @Override
    @Nullable
    public Issue processNode(final JsonNode issueNode) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final Issue issue = IssueAPIParser.getObject(issueNode);
        stopwatch.stop();

        processTime += stopwatch.elapsed(TimeUnit.MILLISECONDS);

        if (issue == null) {
            log.error("null issue after parsing: " + issueNode.toString());
        }

        return issue;
    }

    @Override
    public List<Action> getActions(final Issue issue) throws IOException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final ActionsBuilder actionsBuilder = new ActionsBuilder(actionFactory, issue, startDate, endDate);
        final List<Action> actions = actionsBuilder.buildActions();
        stopwatch.stop();

        processTime += stopwatch.elapsed(TimeUnit.MILLISECONDS);

        actions.stream()
                .map(action -> action.getCustomFieldValues().entrySet())
                .flatMap(Set::stream)
                .filter(v -> !v.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .forEach(customFieldsSeen::add);

        return actions;
    }

    @Override
    public Action getJiraissues(final Action action, final Issue issue) throws IOException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final ActionsBuilder actionsBuilder = new ActionsBuilder(actionFactory, issue, startDate, endDate);
        final Action updatedAction = actionsBuilder.buildJiraIssues(action);
        stopwatch.stop();

        processTime += stopwatch.elapsed(TimeUnit.MILLISECONDS);

        return updatedAction;
    }

    @Override
    public void writeActions(final List<Action> actions) throws IOException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        tsvFileWriter.writeActions(actions);
        stopwatch.stop();

        fileTime += stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    @Override
    public void writeIssue(final Action action) throws IOException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        tsvFileWriter.writeIssue(action);
        stopwatch.stop();

        fileTime += stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    @Override
    public void reset() {
        issuesAPICaller.reset();
    }
}

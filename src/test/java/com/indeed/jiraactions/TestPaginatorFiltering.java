package com.indeed.jiraactions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.indeed.jiraactions.api.response.issue.Issue;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestPaginatorFiltering {
    private static final Issue issue = new Issue();
    private static final DateTime base = DateTime.now();
    private static final Action defaultAction = ImmutableProxy.createProxy(Action.class);
    private static final Action create = ImmutableAction.builder()
            .from(defaultAction)
            .timestamp(base.plusDays(1))
            .build();
    private static final Action update = ImmutableAction.builder()
            .from(defaultAction)
            .timestamp(base.plusDays(2))
            .build();

    @BeforeClass
    public static void init() {
        issue.key = "ABC-123";
    }

    @Test
    public void testFilterActionsEmpty() {
        final List<Action> actions = Paginator.getActionsFilterByLastSeen(ImmutableMap.of(), issue, ImmutableList.of());
        Assert.assertNotNull(actions);
        Assert.assertEquals(0, actions.size());
    }

    @Test
    public void testFilterActionsNotSeen() {
        final Map<String, DateTime> seenIssues = new HashMap<>();
        final List<Action> actions = ImmutableList.of(create, update);

        final List<Action> filteredActions = Paginator.getActionsFilterByLastSeen(seenIssues, issue, actions);

        Assert.assertEquals(actions, filteredActions);
        Assert.assertEquals(1, seenIssues.size());
        Assert.assertEquals(update.getTimestamp(), seenIssues.get(issue.key));
    }

    @Test
    public void testFilterSomeActions() {
        final Map<String, DateTime> seenIssues = new HashMap<>();
        final List<Action> actions = new ArrayList<>(2);
        actions.add(create);

        final List<Action> filteredActions1 = Paginator.getActionsFilterByLastSeen(seenIssues, issue, actions);
        Assert.assertEquals(actions, filteredActions1);
        Assert.assertEquals(1, seenIssues.size());
        Assert.assertEquals(create.getTimestamp(), seenIssues.get(issue.key));

        actions.add(update);
        final List<Action> filteredActions2 = Paginator.getActionsFilterByLastSeen(seenIssues, issue, actions);
        Assert.assertEquals(ImmutableList.of(update), filteredActions2);
        Assert.assertEquals(1, seenIssues.size());
        Assert.assertEquals(update.getTimestamp(), seenIssues.get(issue.key));
    }
}

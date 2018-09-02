package com.indeed.jiraactions;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

public class TestAction {
    private final Action defaultAction = ImmutableProxy.createProxy(Action.class);

    @Test
    public void testCreateDueDateTimeEmpty() {
        final Action action = ImmutableAction.builder().from(defaultAction).dueDate("").build();
        Assert.assertNull(action.getDueDateTime());
    }

    @Test
    public void testCreateDueDate() {
        final DateTime expected = new DateTime(2017, 6, 16, 0, 0, 0, JiraActionsUtil.RAMSES_TIME);
        final String dueDate = "2017-06-15";
        final Action action = ImmutableAction.builder().from(defaultAction).dueDate(dueDate).build();

        Assert.assertEquals(expected, action.getDueDateTime());
    }
}

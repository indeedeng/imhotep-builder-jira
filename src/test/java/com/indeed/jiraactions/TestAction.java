package com.indeed.jiraactions;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

public class TestAction {
    private final Action defaultAction = ImmutableProxy.createProxy(Action.class);

    @Test
    public void testCreateDueDateTimeEmpty() {
        final Action action = ImmutableAction.builder().from(defaultAction).dueDate("").build();
        Assert.assertNull(action.getDueDateTime(DateTimeZone.getDefault()));
    }

    @Test
    public void testCreateDueDate() {
        final DateTime expected = new DateTime(2017, 6, 16, 0, 0, 0, DateTimeZone.forOffsetHours(-6));
        final String dueDate = "2017-06-15";
        final Action action = ImmutableAction.builder().from(defaultAction).dueDate(dueDate).build();

        Assert.assertEquals(expected, action.getDueDateTime(DateTimeZone.forOffsetHours(-6)));
    }
}

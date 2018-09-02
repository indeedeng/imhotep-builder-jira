package com.indeed.jiraactions.api.response.issue.fields;

import org.junit.Assert;
import org.junit.Test;

public class TestDirectCause {
    @Test
    public void testToString() {
        final DirectCause dc = new DirectCause();
        dc.value = "value";
        Assert.assertEquals("value", dc.toString());
    }

    @Test
    public void testChildToString() {
        final DirectCause dc = new DirectCause();
        dc.value = "parent";

        final DirectCause child = new DirectCause();
        child.value = "child";
        dc.child = child;

        Assert.assertEquals("child", child.toString());
        Assert.assertEquals("parent - child", dc.toString());
    }
}

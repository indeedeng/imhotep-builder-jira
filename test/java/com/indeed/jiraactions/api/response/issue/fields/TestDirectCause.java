package com.indeed.jiraactions.api.response.issue.fields;

import org.junit.Assert;
import org.junit.Test;

public class TestDirectCause {
    @Test
    public void testToString() {
        final DirectCause dc = new DirectCause();
        Assert.assertNull(dc.toString());

        dc.value = "value";
        Assert.assertEquals("value", dc.toString());
    }
}

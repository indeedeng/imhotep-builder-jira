package com.indeed.jiraactions.api.response.issue.fields;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class TestIssueSizeEstimate {
    @Test
    public void testToString() {
        final IssueSizeEstimate estimate = new IssueSizeEstimate();
        Assert.assertNull(estimate.toString());

        estimate.value = "value";
        Assert.assertEquals("value", estimate.toString());
    }
}

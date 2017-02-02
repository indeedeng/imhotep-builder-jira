package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields;

import org.junit.Assert;
import org.junit.Test;

public class TestFixVersion {

    @Test
    public void testToString() {
        final FixVersion fixVersion = new FixVersion();
        Assert.assertNull(fixVersion.toString());

        fixVersion.name = "name";
        Assert.assertEquals("name", fixVersion.toString());
    }
}

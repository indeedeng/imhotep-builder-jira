package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class TestComponent {
    @Test
    public void testToString() {
        final Component component = new Component();
        Assert.assertNull(component.toString());

        component.name = "name";
        Assert.assertEquals("name", component.toString());
    }
}

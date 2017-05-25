package com.indeed.jiraactions.api.response.issue.changelog.histories;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class TestItem {
    @Test
    public void testMapping() {
        final Item item = new Item();
        for(final Map.Entry<String, String> entry : Item.jiraFieldMapping.entrySet()) {
            item.setField(entry.getKey());
            Assert.assertEquals(entry.getValue(), item.field);
        }
    }

    @Test
    public void testNotMapping() {
        final Item item = new Item();
        item.setField("Issue Owner");
        Assert.assertEquals("issue-owner", item.field);
    }
}

package com.indeed.jiraactions.api.response.issue.changelog.histories;

import org.junit.Assert;
import org.junit.Test;

public class TestHistory {
    @Test
    public void testCascadingFlattenedEmpty() {
        final Item item = makeItem();
        item.field = "field";
        final History history = new History();
        history.items = new Item[] { item };

        Assert.assertEquals("", history.getItemLastValueFlattened("field", true));
    }

    @Test
    public void testCascadingFlattenedOneLevel() {
        final Item item = makeItem();
        item.field = "field";
        item.toString = "Parent values: Misconfiguration(20661)";
        final History history = new History();
        history.items = new Item[] { item };

        Assert.assertEquals("Misconfiguration", history.getItemLastValueFlattened("field", true));
    }

    @Test
    public void testCascadingFlattened() {
        final Item item = makeItem();
        item.field = "field";
        item.toString = "Parent values: Misconfiguration(20661)Level 1 values: App Config(20669)";
        final History history = new History();
        history.items = new Item[] { item };

        Assert.assertEquals("Misconfiguration - App Config", history.getItemLastValueFlattened("field", true));
    }

    private Item makeItem() {
        final Item item = new Item();
        item.customField = true;
        item.field = "";
        item.from = "";
        item.fromString = "";
        item.to = "";
        item.toString = "";

        return item;
    }
}

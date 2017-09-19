package com.indeed.jiraactions.api.customfields;

import org.junit.Assert;
import org.junit.Test;

public class TestCustomFieldApiParser {
    @Test
    public void testGetItemName() {
        Assert.assertEquals("sysad-categories", CustomFieldApiParser.getItemLabel("SYSAD Categories"));
    }
}

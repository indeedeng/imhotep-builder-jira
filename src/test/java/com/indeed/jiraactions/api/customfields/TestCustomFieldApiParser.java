package com.indeed.jiraactions.api.customfields;

import org.junit.Assert;
import org.junit.Test;

public class TestCustomFieldApiParser {
    @Test
    public void testGetItemName() {
        Assert.assertEquals("sysad-categories", CustomFieldApiParser.getItemLabel("SYSAD Categories"));
    }

    @Test
    public void testGetMultipleLabels() {
        final CustomFieldDefinition definition = ImmutableCustomFieldDefinition.builder()
                .name("Issue Size Estimate")
                .customFieldId("customfield_17090")
                .imhotepFieldName("issuesizeestimate")
                .alternateNames("T-Shirt Size Estimate")
                .build();

        Assert.assertArrayEquals(new String[] {"issue-size-estimate", "t-shirt-size-estimate"}, CustomFieldApiParser.getItemLabels(definition));
    }
}

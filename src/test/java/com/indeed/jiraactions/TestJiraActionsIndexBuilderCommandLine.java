package com.indeed.jiraactions;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

public class TestJiraActionsIndexBuilderCommandLine {
    @Test
    public void testTimeZoneEmpty() {
        final PropertiesConfiguration config = new PropertiesConfiguration();
        Assert.assertEquals(DateTimeZone.UTC, JiraActionsIndexBuilderCommandLine.getDateTimeZone(config, "anykey"));
    }
}

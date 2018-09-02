package com.indeed.jiraactions.api;

import com.indeed.jiraactions.JiraActionsUtil;
import org.junit.Assert;
import org.junit.Test;

public class TestIssuesApiCaller {
    @Test
    public void testJiraFormatter() {
        // Hack to account for when Austin is in Ramses time
        if(IssuesAPICaller.JIRA_TIME_ZONE.equals(JiraActionsUtil.RAMSES_TIME)) {
            return;
        }
        Assert.assertEquals("2018-04-01 01:00", IssuesAPICaller.getDateStringInJiraTime("2018-04-01"));
    }
}

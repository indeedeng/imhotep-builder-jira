package com.indeed.jiraactions.jiraissues;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestJiraIssuesProcess {
    final DateTime date = DateTime.parse("2019-01-01");
    final int monthRange = 6;
    final JiraIssuesProcess process = new JiraIssuesProcess(date, monthRange);
    final List<String[]> newIssues = new ArrayList<>();
    final String[] fields = {"issuekey", "status", "time", "issueage", "totaltime_open", "totaltime_pending_triage", "totaltime_in_progress", "totaltime_closed"};

    @Before
    public void setup() {
        setupNewIssues();

        process.setNewIssues(newIssues);
        process.setNewFields(Arrays.stream(fields).collect(Collectors.toList()));
        process.setOldFields(Arrays.stream(fields).collect(Collectors.toList()));
        process.convertToMap();
    }

    @Test
    public void testCompare() {
        // The issues being passed in would be the old issues from the previous day.
        final String[] issue1 = {"A", "Pending Triage", "1546236000", "0", "0", "0", "0", "0"};   // Test Replacing Process
        final Map<String, String> output1 = process.compareAndUpdate(issue1);
        final String[] expected1 = {"A", "In Progress", "1546322400", "86400", "0", "86400", "0", "0"};
        Assert.assertEquals(expected1, output1.values().toArray());

        final String[] issue2 = {"B", "Closed", "1546236000", "0", "0", "0", "0", "0"};  // Test Updating Process - Although we could have tested the actual update method, this also checks if there is a new instance of that issue and would be a better case.
        final Map<String, String> output2 = process.compareAndUpdate(issue2);
        final String[] expected2 = {"B", "Closed", "1546322400", "86400", "0", "0", "0", "86400"};
        Assert.assertEquals(expected2, output2.values().toArray());

        final String[] issue3 = {"", "", "1546236000", "0", "0", "0", "0", "0"};       // Test blank issuekey and status
        final Map<String, String> output3 = process.compareAndUpdate(issue3);
        final String[] expected3 = {"", "", "1546322400", "86400", "0", "0", "0", "0"};
        Assert.assertEquals(expected3, output3.values().toArray());
    }

    @Test
    public void testGetRemainingIssues() {
        final String[] issue1 = {"A", "Pending Triage", "1546236000", "0", "0", "0", "0", "0"};
        final String[] issue2 = {"B", "Closed", "1546236000", "0", "0", "0", "0", "0"};
        process.compareAndUpdate(issue1);   // The new issue is removed if it replaces when passed in the compare method.
        process.compareAndUpdate(issue2);

        final List<Map<String, String>> remainingIssues = process.getRemainingIssues();
        Assert.assertEquals(1, remainingIssues.size());

        final Map<String, String> remainingIssue = remainingIssues.get(0);
        final String[] expectedIssue = {"C", "Open", "1546322400", "86400", "0", "0", "0", "0"};
        Assert.assertEquals(expectedIssue, remainingIssue.values().toArray());
    }

    @Test
    public void testNewFields() {
        final JiraIssuesProcess process = new JiraIssuesProcess(date, monthRange);

        final List<String[]> newIssues = new ArrayList<>();
        final String[] newFields = {"issuekey", "status", "time", "issueage", "totaltime_closed", "totaltime_open"};
        newIssues.add(newFields);

        final String[] oldFields = {"issuekey", "status", "time", "issueage", "totaltime_open"};
        process.setNewIssues(newIssues);
        process.setOldFields(Arrays.stream(oldFields).collect(Collectors.toList()));
        process.setNewFields(Arrays.stream(newFields).collect(Collectors.toList()));

        final String[] issue = {"A", "Open", "1546236000", "0", "0"};
        final Map<String, String> output = process.compareAndUpdate(issue);
        final String[] expected = {"A", "Open", "1546322400", "86400", "0", "86400"};      // If there is a new field it will set "0" as the value for that field
        Assert.assertEquals(expected, output.values().toArray());
    }

    @Test
    public void testNonApiStatuses() {
        final String[] issue = {"D", "Accepted", "1546236000", "0", "0", "0", "0", "0"};       // "Accepted" is in the API but it isn't in the fields that were set for these tests
        process.compareAndUpdate(issue);
        Assert.assertEquals("Accepted", process.getNonApiStatuses().get(0));
    }

    @Test
    public void testStatusReplacement() {
        final JiraIssuesProcess process = new JiraIssuesProcess(date, monthRange);

        final List<String[]> newIssues = new ArrayList<>();
        final String[] newFields = {"issuekey", "status", "time", "issueage", "totaltime_c", "totaltime_a"};
        newIssues.add(newFields);

        final String[] oldFields = {"issuekey", "status", "time", "issueage", "totaltime_a", "totaltime_b"};      // b is replaced by c and is placed in a different order
        process.setNewIssues(newIssues);
        process.setOldFields(Arrays.stream(oldFields).collect(Collectors.toList()));
        process.setNewFields(Arrays.stream(newFields).collect(Collectors.toList()));

        final String[] issue = {"A", "a", "1546236000", "0", "0", "1"};        // There currently isn't a way to check which statuses get replaced so the best it can do is "remove" the old one and set 0 as the new one
        final Map<String, String> output = process.compareAndUpdate(issue);
        final String[] expected = {"A", "a", "1546322400", "86400", "0", "86400"};
        Assert.assertEquals(expected, output.values().toArray());
    }

    @Test
    public void testDateFilter() {
        // start date is 2019-01-01
        final JiraIssuesProcess process = new JiraIssuesProcess(date, monthRange);

        final List<String[]> newIssues = new ArrayList<>();
        final String[] newFields = {"issuekey", "status", "time", "issueage", "lastupdated"};
        newIssues.add(newFields);

        final String[] oldFields = {"issuekey", "status", "time", "issueage", "lastupdated"};
        process.setNewIssues(newIssues);
        process.setOldFields(Arrays.stream(oldFields).collect(Collectors.toList()));
        process.setNewFields(Arrays.stream(newFields).collect(Collectors.toList()));

        final String[] issue1 = {"A", "Closed", "1546236000", "0", "20180101"};        // last updated 2018-01-01.
        final Map<String, String> output1 = process.compareAndUpdate(issue1);
        Assert.assertNull(output1);

        final String[] issue2 = {"B", "Open", "1546236000", "0", "20180801"};     // last updated 2018-08-01
        final Map<String, String> output2 = process.compareAndUpdate(issue2);
        final String[] expected2 = {"B", "Open", "1546322400", "86400", "20180801"};
        Assert.assertEquals(expected2, output2.values().toArray());



    }

    public void setupNewIssues() {
        newIssues.add(fields);
        String[] issue1 = {"A", "In Progress", "1546322400", "86400", "0", "86400", "0", "0"};       // Should replace previous day's issue
        newIssues.add(issue1);
        String[] issue2 = {"C", "Open", "1546322400", "86400", "0", "0", "0", "0"};      // Should be added
        newIssues.add(issue2);
    }

}
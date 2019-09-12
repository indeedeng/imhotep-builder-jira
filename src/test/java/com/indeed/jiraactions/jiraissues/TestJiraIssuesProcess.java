package com.indeed.jiraactions.jiraissues;

import com.google.common.collect.ImmutableList;
import com.indeed.jiraactions.JiraActionsUtil;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestJiraIssuesProcess {
    final DateTime start = JiraActionsUtil.parseDateTime("2019-01-01 00:00:00");
    final DateTime end = JiraActionsUtil.parseDateTime("2019-01-02 00:00:00");
    final String unixtime = JiraActionsUtil.getUnixTimestamp(end);
    final int monthRange = 6;
    final JiraIssuesProcess process = new JiraIssuesProcess(start, end, monthRange);
    final List<String[]> newIssues = new ArrayList<>();
    final List<String> fields = ImmutableList.of("issuekey", "status", "time", "issueage", "totaltime_open", "totaltime_pending_triage", "totaltime_in_progress", "totaltime_closed");

    @Before
    public void setup() {
        setupNewIssues();

        process.setNewIssues(newIssues);
        process.setNewFields(fields);
        process.setOldFields(fields);
        process.convertToMap();
    }

    // These issues replicate the new issues from jiraactions and will only be used by testCompare(), testGetRemainingIssues() and testNonApiStatuses().
    public void setupNewIssues() {
        final String[] newIssue1 = {"A", "In Progress", unixtime, "86400", "0", "86400", "0", "0"};       // Should replace previous day's issue
        newIssues.add(newIssue1);
        final String[] newIssue2 = {"C", "Open", unixtime, "86400", "0", "0", "0", "0"};      // Should be added
        newIssues.add(newIssue2);
    }

    @Test
    public void testCompare() {
        // The issues being passed in would be the old issues from the previous day.
        final String[] issue1 = {"A", "Pending Triage", "0", "0", "0", "0", "0", "0"};   // Test Replacing Process
        final Map<String, String> output1 = process.compareAndUpdate(issue1);
        final String[] expected1 = {"A", "In Progress", unixtime, "86400", "0", "86400", "0", "0"};
        Assert.assertEquals(expected1, output1.values().toArray());

        final String[] issue2 = {"B", "Closed", "0", "0", "0", "0", "0", "0"};  // Test Updating Process - Although we could have tested the actual update method, this also checks if there is a new instance of that issue and would be a better case.
        final Map<String, String> output2 = process.compareAndUpdate(issue2);
        final String[] expected2 = {"B", "Closed", unixtime, "86400", "0", "0", "0", "86400"};
        Assert.assertEquals(expected2, output2.values().toArray());

        final List<Map<String, String>> remainingIssues = process.getRemainingIssues();
        Assert.assertEquals(1, remainingIssues.size());

        final Map<String, String> remainingIssue = remainingIssues.get(0);
        final String[] expectedIssue = {"C", "Open", unixtime, "86400", "0", "0", "0", "0"};    // Test Adding Process - It uses the issues from setupNewIssues in which A was already replaced earlier in the test so C is the remaining issue.
        Assert.assertEquals(expectedIssue, remainingIssue.values().toArray());
    }

    @Test
    public void testBlankIssue() {
        final String[] issue3 = {"", "", "0", "0", "0", "0", "0", "0"};       // Test blank issuekey and status
        final Map<String, String> output3 = process.compareAndUpdate(issue3);
        final String[] expected3 = {"", "", unixtime, "86400", "0", "0", "0", "0"};
        Assert.assertEquals(expected3, output3.values().toArray());
    }

    @Test
    public void testNonApiStatuses() {
        final String[] issue = {"D", "Accepted", "0", "0", "0", "0", "0", "0"};       // Technically, "Accepted" is in the API but it isn't in the fields that were set for these tests so it will be added.
        process.compareAndUpdate(issue);
        Assert.assertEquals("Accepted", process.getNonApiStatuses().get(0));
    }

    @Test
    public void testNewFields() {
        final JiraIssuesProcess process = new JiraIssuesProcess(start, end, monthRange);

        final List<String> oldFields = ImmutableList.of("issuekey", "status", "time", "issueage", "totaltime_open");
        final List<String> newFields = ImmutableList.of("issuekey", "status", "time", "issueage", "totaltime_closed", "totaltime_open");

        process.setNewIssues(new ArrayList<>());
        process.setOldFields(oldFields);
        process.setNewFields(newFields);

        final String[] issue = {"A", "Open", "0", "0", "0"};
        final Map<String, String> output = process.compareAndUpdate(issue);
        final String[] expected = {"A", "Open", unixtime, "86400", "0", "86400"};      // If there is a new status field it will set "0" as the value for that field
        Assert.assertEquals(expected, output.values().toArray());
    }

    @Test
    public void testStatusReplacement() {
        final JiraIssuesProcess process = new JiraIssuesProcess(start, end, monthRange);

        final List<String> oldFields = ImmutableList.of("issuekey", "status", "time", "issueage", "totaltime_a", "totaltime_b");      // b is replaced by c and is placed in a different order
        final List<String> newFields = ImmutableList.of("issuekey", "status", "time", "issueage", "totaltime_c", "totaltime_a");

        process.setNewIssues(new ArrayList<>());
        process.setOldFields(oldFields);
        process.setNewFields(newFields);

        final String[] issue = {"A", "a", "0", "0", "0", "1"};        // There currently isn't a way to check which statuses get replaced in the API so the best it can do is "remove" the old one and set 0 as the new one
        final Map<String, String> output = process.compareAndUpdate(issue);
        final String[] expected = {"A", "a", unixtime, "86400", "0", "86400"};
        Assert.assertEquals(expected, output.values().toArray());
    }

    @Test
    public void testDateFilter() {
        // start start is 2019-01-01
        final JiraIssuesProcess process = new JiraIssuesProcess(start, end, monthRange);

        final List<String> fields = ImmutableList.of("issuekey", "status", "time", "issueage", "lastupdated");  // We are using the same fields for this test

        process.setNewIssues(new ArrayList<>());
        process.setOldFields(fields);
        process.setNewFields(fields);

        final String[] issue1 = {"A", "Closed", "0", "0", "20180101"};        // last updated 2018-01-01.
        final Map<String, String> output1 = process.compareAndUpdate(issue1);
        Assert.assertNull(output1);

        final String[] issue2 = {"B", "Open", "0", "0", "20180801"};     // last updated 2018-08-01
        final Map<String, String> output2 = process.compareAndUpdate(issue2);
        final String[] expected2 = {"B", "Open", unixtime, "86400", "20180801"};
        Assert.assertEquals(expected2, output2.values().toArray());
    }

    @Test
    @Ignore // TODO: This broke when we changed from start-date to end-date.
    public void testDaylightSavings() {
        final DateTime start1 = JiraActionsUtil.parseDateTime("2018-03-12 00:00:00");    // Daylight savings for 2018 begins: March, 11
        final DateTime end1 = JiraActionsUtil.parseDateTime("2018-03-13 00:00:00");    // Daylight savings for 2018 begins: March, 11
        final String start1unixtime = JiraActionsUtil.getUnixTimestamp(start1);
        final DateTime start2 = JiraActionsUtil.parseDateTime("2018-11-05 00:00:00");    // Daylight savings for 2018 ends: November, 4
        final DateTime end2 = JiraActionsUtil.parseDateTime("2018-11-06 00:00:00");    // Daylight savings for 2018 ends: November, 4
        final String start2unixtime = JiraActionsUtil.getUnixTimestamp(start2);

        final JiraIssuesProcess process1 = new JiraIssuesProcess(start1, end1, 12);
        final JiraIssuesProcess process2 = new JiraIssuesProcess(start2, end2, 12);

        final List<String> fields = ImmutableList.of("issuekey", "status", "time", "issueage");

        process1.setNewIssues(new ArrayList<>());
        process1.setOldFields(fields);
        process1.setNewFields(fields);
        process2.setNewIssues(new ArrayList<>());
        process2.setOldFields(fields);
        process2.setNewFields(fields);

        final String[] issue1 = {"A", "Open", "0", "0"};
        final Map<String, String> output1 = process1.compareAndUpdate(issue1);
        final String[] expected1 = {"A", "Open", start1unixtime, "86400"};
        Assert.assertEquals(expected1, output1.values().toArray());

        final String[] issue2 = {"B", "Open", "0", "0"};
        final Map<String, String> output2 = process2.compareAndUpdate(issue2);
        final String[] expected2 = {"B", "Open", start2unixtime, "86400"};
        Assert.assertEquals(expected2, output2.values().toArray());

    }

}

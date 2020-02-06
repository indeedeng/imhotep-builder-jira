package com.indeed.jiraactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.jiraactions.api.IssueAPIParser;
import com.indeed.jiraactions.api.response.issue.Issue;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;

public class IssueTest {
    @Test
    public void testComponentsInitialValue() throws IOException {
        try (final InputStream stream = getClass().getResourceAsStream("/ENGPLANS-10.json")) {
            Assert.assertNotNull(stream);
            final JsonNode node = new ObjectMapper().readTree(stream);
            Issue issue = IssueAPIParser.getObject(node);

            Assert.assertThat(
                    issue.initialValue("component"),
                    equalTo("Eng"));
        }
    }

    /**
     * When a component in the current state is added in the history, then
     *  it should not appear in the initial value.
     */
    @Test
    public void testComponentInCurrentStateAddedInHistory() throws IOException {
        try (final InputStream stream = getClass().getResourceAsStream("/ENGPLANS-10.testComponentInCurrentStateAddedInHistory.json")) {
            Assert.assertNotNull(stream);
            final JsonNode node = new ObjectMapper().readTree(stream);
            Issue issue = IssueAPIParser.getObject(node);

            Assert.assertThat(
                    issue.initialValue("component"),
                    equalTo(""));
        }
    }

    /**
     * When the current state contains multiple components and only one is added in history, then
     *   the intial state should consist of the other one.
     */
    @Test
    public void testMultipleComponentsMixedHistory() throws IOException {
        try (final InputStream stream = getClass().getResourceAsStream("/ENGPLANS-10.testMultipleComponentsMixedHistory.json")) {
            Assert.assertNotNull(stream);
            final JsonNode node = new ObjectMapper().readTree(stream);
            Issue issue = IssueAPIParser.getObject(node);

            Assert.assertThat(
                    issue.initialValue("component"),
                    equalTo("Specialized"));
        }
    }

    /**
     * When the current state contains multiple components and none are present in history, make sure the
     *  initial value contains both
     */
    @Test
    public void testMultipleComponents() throws IOException {
        try (final InputStream stream = getClass().getResourceAsStream("/ENGPLANS-10.testMultipleComponents.json")) {
            Assert.assertNotNull(stream);
            final JsonNode node = new ObjectMapper().readTree(stream);
            Issue issue = IssueAPIParser.getObject(node);

            Assert.assertThat(
                    "Expected multiple components to be present in the initialValue, and in the order specified",
                    issue.initialValue("component"),
                    equalTo("Eng|Specialized"));
        }
    }

    /**
     * When the current state contains multiple components and none are present in history, make sure the
     *  initial value contains both
     */
    @Test
    public void testComponentRemoved() throws IOException {
        try (final InputStream stream = getClass().getResourceAsStream("/ENGPLANS-10.testComponentRemoved.json")) {
            Assert.assertNotNull(stream);
            final JsonNode node = new ObjectMapper().readTree(stream);
            Issue issue = IssueAPIParser.getObject(node);

            Assert.assertThat(
                    "Expected removed component to be restored to the initialValue, and in the order specified",
                    issue.initialValue("component"),
                    equalTo("Specialized|Eng"));
        }
    }
}

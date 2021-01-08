package com.indeed.jiraactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.indeed.jiraactions.api.IssueAPIParser;
import com.indeed.jiraactions.api.customfields.CustomFieldApiParser;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.response.issue.Issue;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

public class ActionFactoryTest {
    private final UserLookupService userLookupService = new FriendlyUserLookupService();

    @Test
    public void testCreation() throws IOException {
        final ActionFactory factory = newActionFactory();

        try (final InputStream stream = getClass().getResourceAsStream("/ENGPLANS-10.json")) {
            Assert.assertNotNull(stream);
            final JsonNode node = new ObjectMapper().readTree(stream);
            final Issue issue = IssueAPIParser.getObject(node);
            final Action action = factory.create(issue);
            Assert.assertEquals(new DateTime(2009, 2, 12, 17, 40, 27, DateTimeZone.forOffsetHours(-6)), action.getCreatedDate());
            Assert.assertEquals(Optional.empty(), action.getResolutionDate());
        }
    }

    @Test
    /*
     * Components that are added to an issue when the issue is first created will never appear in
     *  the changelog. Make sure that we are adding this to the issue.
     */
    public void testComponentsCreatedWithIssue() throws IOException {
        final ActionFactory factory = newActionFactory();

        try (final InputStream stream = getClass().getResourceAsStream("/ENGPLANS-10.json")) {
            Assert.assertNotNull(stream);
            final JsonNode node = new ObjectMapper().readTree(stream);
            final Issue issue = IssueAPIParser.getObject(node);
            final Action action = factory.create(issue);

            Assert.assertThat(action.getComponents(), is(equalTo(ImmutableList.of("Eng"))));
        }
    }

    /**
     * Fix Versions that are added to an issue when the issue is first created will never appear in
     *  the changelog. Make sure that we are adding this to the issue.
     */
    @Test
    public void testFixVersionsCreatedWithIssueAndAddedLater() throws IOException {
        final ActionFactory factory = newActionFactory();

        try (final InputStream stream = getClass().getResourceAsStream("/ENGPLANS-10.testMultipleFixVersionsMixedHistory.json")) {
            Assert.assertNotNull(stream);
            final JsonNode node = new ObjectMapper().readTree(stream);
            final Issue issue = IssueAPIParser.getObject(node);
            Action action = factory.create(issue);

            Assert.assertThat(
                    "Expected the initial value of the fixVersions to include the value added on creation and remove the value added in history.",
                    action.getFixVersions(), is(equalTo(ImmutableList.of("Private launch"))));

            for (final History history: issue.changelog.histories) {
                action = factory.update(action, history);
            }

            Assert.assertThat(
                    "Expected the initial value of the fixVersions to include the value added on creation and remove the value added in history.",
                    action.getFixVersions(), is(equalTo(ImmutableList.of("Private launch", "Public launch"))));

            Assert.assertThat(
                    "Expected the initial value of the fixVersions to include the value added on creation and remove the value added in history.",
                    action.getFixVersionsJoined(), is(equalTo("Private launch|Public launch")));

        }
    }

    @Test
    public void testExtractValidProjectKeyFromIssueKey() {
        final ActionFactory factory = newActionFactory();
        Assert.assertEquals("ABC", factory.getProjectkeyFromIssuekey("ABC-123"));
    }

    @Test
    public void testExtractShortestProjectKeyFromIssueKey() {
        final ActionFactory factory = newActionFactory();
        Assert.assertEquals("A", factory.getProjectkeyFromIssuekey("A-189"));
    }

    @Test
    public void testExtractMissingProjectKeyFromIssueKey() {
        final ActionFactory factory = newActionFactory();
        Assert.assertThrows(IllegalArgumentException.class, () -> factory.getProjectkeyFromIssuekey("-123"));
    }

    @Test
    public void testExtractProjectKeyFromMalformedIssueyKey() {
        final ActionFactory factory = newActionFactory();
        Assert.assertThrows(IllegalArgumentException.class, () -> factory.getProjectkeyFromIssuekey("123"));
    }

    private ActionFactory newActionFactory() {
        final JiraActionsIndexBuilderConfig config = EasyMock.createNiceMock(JiraActionsIndexBuilderConfig.class);
        EasyMock.expect(config.getCustomFields()).andReturn(new CustomFieldDefinition[0]).anyTimes();
        EasyMock.replay(config);

        return new ActionFactory(userLookupService, new CustomFieldApiParser(userLookupService), config);
    }
}

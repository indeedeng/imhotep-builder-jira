package com.indeed.jiraactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.indeed.jiraactions.api.IssueAPIParser;
import com.indeed.jiraactions.api.customfields.CustomFieldApiParser;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.response.issue.Issue;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

public class ActionFactoryTest {
    private final UserLookupService userLookupService = new FriendlyUserLookupService();

    @Test
    public void testCreation() throws IOException {
        ActionFactory factory = newActionFactory();

        try (final InputStream stream = getClass().getResourceAsStream("/ENGPLANS-10.json")) {
            Assert.assertNotNull(stream);
            final JsonNode node = new ObjectMapper().readTree(stream);
            Issue issue = IssueAPIParser.getObject(node);
            final Action action = factory.create(issue);

            Assert.assertEquals("2009-02-12", action.getCreatedDate());
            Assert.assertEquals(20090212, action.getCreatedDateLong());
            Assert.assertEquals(20090212174027L, action.getCreatedDateTimeLong());
            Assert.assertEquals(1234482027, action.getCreatedDateTimestamp());

            Assert.assertEquals("2009-02-12", action.getResolutionDate());
            Assert.assertEquals(20090212, action.getResolutionDateLong());
            Assert.assertEquals(20090212184850L, action.getResolutionDateTimeLong());
            Assert.assertEquals(1234486130, action.getResolutionDateTimestamp());
        }
    }

    @Test
    /**
     * Components that are added to an issue when the issue is first created will never appear in
     *  the changelog. Make sure that we are adding this to the issue.
     *
     * TODO - It's not quite accurate to add these to creation Action and leave them there. Instead,
     *  we would ideally determine which components were present initially by process of elimination.
     *  This problem applies much more generally than just to Components, however... it really affects
     *  the accuracy of every field that can be set when the issue is created and thus have a value
     *  other than what the first item in the history indicates. That's a very significant change
     *  to the operation of this mechanism, though, and best left to a more complete effort.
     */
    public void testComponentsCreatedWithIssue() throws IOException {
        ActionFactory factory = newActionFactory();

        try (final InputStream stream = getClass().getResourceAsStream("/ENGPLANS-10.json")) {
            Assert.assertNotNull(stream);
            final JsonNode node = new ObjectMapper().readTree(stream);
            Issue issue = IssueAPIParser.getObject(node);
            final Action action = factory.create(issue);

            Assert.assertThat(action.getComponents(), is(equalTo(ImmutableList.of("Eng"))));
        }
    }

    private ActionFactory newActionFactory() {
        final JiraActionsIndexBuilderConfig config = EasyMock.createNiceMock(JiraActionsIndexBuilderConfig.class);
        EasyMock.expect(config.getCustomFields()).andReturn(new CustomFieldDefinition[0]).anyTimes();
        EasyMock.replay(config);

        return new ActionFactory(userLookupService, new CustomFieldApiParser(userLookupService), config);
    }
}

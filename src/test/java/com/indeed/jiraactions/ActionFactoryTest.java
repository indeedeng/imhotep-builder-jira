package com.indeed.jiraactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private ActionFactory newActionFactory() {
        final JiraActionsIndexBuilderConfig config = EasyMock.createNiceMock(JiraActionsIndexBuilderConfig.class);
        EasyMock.expect(config.getCustomFields()).andReturn(new CustomFieldDefinition[0]).anyTimes();
        EasyMock.replay(config);

        return new ActionFactory(userLookupService, new CustomFieldApiParser(userLookupService), config);
    }
}

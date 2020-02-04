package com.indeed.jiraactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.indeed.jiraactions.api.IssueAPIParser;
import com.indeed.jiraactions.api.customfields.CustomFieldApiParser;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinitionParser;
import com.indeed.jiraactions.api.customfields.CustomFieldValue;
import com.indeed.jiraactions.api.response.issue.Issue;
import org.easymock.EasyMock;
import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.OptionalInt;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

public class CustomFieldOutputterTest {
    private final UserLookupService userLookupService = new FriendlyUserLookupService();

    @Test
    public void testDateTimeExpansion() throws IOException {
        final CustomFieldOutputter outputter = new CustomFieldOutputter(new OutputFormatter(OptionalInt.empty()));

        try (
                final InputStream issueStream = getClass().getResourceAsStream("/ENGPLANS-10.json");
                final InputStream fieldStream = getClass().getResourceAsStream("/customfields/date-time.json")
        ) {
            final CustomFieldDefinition[] definitions = CustomFieldDefinitionParser.parseCustomFields(fieldStream);
            Assert.assertEquals(1, definitions.length);

            final JsonNode node = new ObjectMapper().readTree(issueStream);
            final Issue issue = IssueAPIParser.getObject(node);
            final Action action = newActionFactory(definitions).create(issue);

            final CustomFieldDefinition definition = definitions[0];
            final CustomFieldValue value = action.getCustomFieldValues().get(definition);

            final List<String> headers = definition.getHeaders();
            final List<String> values = outputter.getValues(value);

            Assert.assertEquals("Headers and values must have same dimension", headers.size(), values.size());

            final String expectedFieldName = "custom";
            Assert.assertThat(headers, is(equalTo(ImmutableList.of(
                    expectedFieldName + "date",
                    expectedFieldName + "datetime",
                    expectedFieldName + "timestamp"))));

            final DateTime expectedTime = DateTime.parse("2010-03-22T14:07:43.000-0500")
                    .withZone(DateTimeZone.forOffsetHours(-6));
            Assert.assertThat(values, is(equalTo(ImmutableList.of(
                    expectedTime.toString("yyyyMMdd"),
                    expectedTime.toString("yyyyMMddHHmmss"),
                    String.valueOf(expectedTime.getMillis())))));
        }
    }

    private ActionFactory newActionFactory(final CustomFieldDefinition[] definitions) {
        final JiraActionsIndexBuilderConfig config = EasyMock.createNiceMock(JiraActionsIndexBuilderConfig.class);
        EasyMock.expect(config.getCustomFields()).andReturn(definitions).anyTimes();
        EasyMock.replay(config);

        return new ActionFactory(userLookupService, new CustomFieldApiParser(userLookupService), config);
    }
}

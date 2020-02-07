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
            Assert.assertEquals(2, definitions.length);

            final JsonNode node = new ObjectMapper().readTree(issueStream);
            final Issue issue = IssueAPIParser.getObject(node);
            final Action action = newActionFactory(definitions).create(issue);

            final CustomFieldDefinition definitionWithValue = definitions[0];
            final CustomFieldValue valueAppearingInHistory = action.getCustomFieldValues().get(definitionWithValue);

            final List<String> headers = definitionWithValue.getHeaders();
            final List<String> values = outputter.getValues(valueAppearingInHistory);

            Assert.assertEquals("Headers and values must have same dimension", headers.size(), values.size());

            final String expectedFieldName = "custom";
            Assert.assertThat(headers, is(equalTo(ImmutableList.of(
                    "int " + expectedFieldName + "date",
                    "string " + expectedFieldName + "datetime",
                    "int " + expectedFieldName + "timestamp"))));

            final DateTime expectedTime = DateTime.parse("2010-03-22T14:07:43.000-0500")
                    .withZone(DateTimeZone.forOffsetHours(-6));
            Assert.assertThat(values, is(equalTo(ImmutableList.of(
                    expectedTime.toString("yyyyMMdd"),
                    expectedTime.toString("yyyy-MM-dd HH:mm:ss"),
                    String.valueOf(expectedTime.getMillis())))));


            final CustomFieldDefinition definitionNotAppearingInHistory = definitions[1];
            Assert.assertEquals("Custom DateTime with no value in ENGPLANS-10", definitionNotAppearingInHistory.getName());
            final CustomFieldValue valueNotAppearingInHistory = action.getCustomFieldValues().get(definitionNotAppearingInHistory);

            final List<String> headersForDefinitionNotInHistory = definitionNotAppearingInHistory.getHeaders();
            final List<String> valuesForDefinitionNotInHistory = outputter.getValues(valueNotAppearingInHistory);

            Assert.assertEquals("Headers and values must have same dimension",
                    headersForDefinitionNotInHistory.size(),
                    valuesForDefinitionNotInHistory.size());

            Assert.assertThat(headersForDefinitionNotInHistory, is(equalTo(ImmutableList.of(
                    "int " + expectedFieldName + "date",
                    "string " + expectedFieldName + "datetime",
                    "int " + expectedFieldName + "timestamp"))));

            Assert.assertThat(
                    "Custom fields that do not appear in history should result in blank values",
                    valuesForDefinitionNotInHistory, is(equalTo(ImmutableList.of("","",""))));
        }
    }

    private ActionFactory newActionFactory(final CustomFieldDefinition[] definitions) {
        final JiraActionsIndexBuilderConfig config = EasyMock.createNiceMock(JiraActionsIndexBuilderConfig.class);
        EasyMock.expect(config.getCustomFields()).andReturn(definitions).anyTimes();
        EasyMock.replay(config);

        return new ActionFactory(userLookupService, new CustomFieldApiParser(userLookupService), config);
    }
}

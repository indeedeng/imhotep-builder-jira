package com.indeed.jiraactions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition.MultiValueFieldConfiguration;
import com.indeed.jiraactions.api.customfields.CustomFieldValue;
import com.indeed.jiraactions.api.customfields.ImmutableCustomFieldDefinition;
import com.indeed.jiraactions.api.response.issue.ImmutableUser;
import com.indeed.jiraactions.api.response.issue.User;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

public class TSVSpecBuilderTest extends EasyMockSupport {
    Action action;
    TSVSpecBuilder builder;
    CustomFieldOutputter customFieldOutputter;
    private final OutputFormatter outputFormatter = new OutputFormatter(OptionalInt.empty());

    @Before
    public void before() {
        action = createNiceMock(Action.class);
        customFieldOutputter = createNiceMock(CustomFieldOutputter.class);

        builder = new TSVSpecBuilder(outputFormatter, customFieldOutputter);
    }

    @Test
    public void testOne() {
        builder.addColumn("issuetype", Action::getIssuetype);
        EasyMock.expect(action.getIssuetype()).andReturn("result");
        verifyHeadersAndValues(
                ImmutableList.of("issuetype"),
                ImmutableList.of("result")
        );
    }

    @Test
    public void testUser() {
        builder.addUserColumns("actor", Action::getActor);
        final User actor = ImmutableUser.builder()
                .displayName("displayName")
                .name("name")
                .key("key")
                .addGroups("group1", "group2")
                .build();
        EasyMock.expect(action.getActor()).andReturn(actor).anyTimes();
        verifyHeadersAndValues(
                ImmutableList.of("actor", "actorusername", "actorgroups*|"),
                ImmutableList.of("displayName", "name", "group1|group2")
        );
    }

    @Test
    public void testTime() {
        builder.addTimeColumn("time", Action::getTimestamp);
        final long time = 1521835092;
        EasyMock.expect(action.getTimestamp()).andReturn(new DateTime(time*1000)).anyTimes();
        verifyHeadersAndValues(
                ImmutableList.of("time"),
                ImmutableList.of(String.valueOf(time))
        );
    }

    @Test
    public void testLong() {
        builder.addLongColumn("issueage", Action::getIssueage);
        final long age = 1521835092;
        EasyMock.expect(action.getIssueage()).andReturn(age).anyTimes();
        verifyHeadersAndValues(
                ImmutableList.of("issueage"),
                ImmutableList.of(String.valueOf(age))
        );
    }

    @Test
    public void testCustomField() {
        final CustomFieldDefinition fieldDefinition = ImmutableCustomFieldDefinition.builder()
                .imhotepFieldName("custom")
                .name("my custom field")
                .customFieldId("1234")
                .build();
        builder.addCustomFieldColumns(fieldDefinition);
        final CustomFieldValue value = newCustomFieldValue(ImmutableList.of("customvalue"));
        EasyMock.expect(action.getCustomFieldValues()).andReturn(ImmutableMap.of(fieldDefinition, value));
        verifyHeadersAndValues(
                ImmutableList.of("custom"),
                ImmutableList.of("customvalue")
        );
    }

    @Test
    public void testMultiCustomField() {
        final CustomFieldDefinition fieldDefinition = ImmutableCustomFieldDefinition.builder()
                .imhotepFieldName("custom")
                .name("my custom field")
                .customFieldId("1234")
                .multiValueFieldConfiguration(MultiValueFieldConfiguration.SEPARATE)
                .build();
        builder.addCustomFieldColumns(fieldDefinition);
        final CustomFieldValue value = newCustomFieldValue(ImmutableList.of("customvalue1", "customvalue2"));
        EasyMock.expect(action.getCustomFieldValues()).andReturn(ImmutableMap.of(fieldDefinition, value)).anyTimes();
        verifyHeadersAndValues(
                ImmutableList.of("custom1", "custom2"),
                ImmutableList.of("customvalue1", "customvalue2")
        );
    }

    @Test
    public void testDateTimeCustomField() {
        final CustomFieldDefinition fieldDefinition = ImmutableCustomFieldDefinition.builder()
                .imhotepFieldName("custom")
                .name("my custom field")
                .customFieldId("1234")
                .multiValueFieldConfiguration(MultiValueFieldConfiguration.DATETIME)
                .build();
        builder.addCustomFieldColumns(fieldDefinition);
        final String isoDateTime = "2010-03-22T14:07:43.000-0500";
        final DateTime dateTime = DateTime.parse(isoDateTime).withZone(DateTimeZone.forOffsetHours(-6));

        // With the Action class mocked, this unit test is a little meaningless.
        final CustomFieldValue value = newCustomFieldValue(ImmutableList.of(
                dateTime.toString("yyyyMMdd"),
                dateTime.toString("yyyy-MM-dd HH:mm:ss"),
                String.valueOf(dateTime.getMillis())));

        EasyMock.expect(
                action.getCustomFieldValues())
                        .andReturn(
                                ImmutableMap.of(fieldDefinition, value))
                        .anyTimes();

        verifyHeadersAndValues(
                ImmutableList.of("int customdate", "string customdatetime", "int customtimestamp"),
                ImmutableList.of(
                        dateTime.toString("yyyyMMdd"),
                        dateTime.toString("yyyy-MM-dd HH:mm:ss"),
                        String.valueOf(dateTime.getMillis()))
        );
    }

    @Test
    public void testUserCustomField() {
        final CustomFieldDefinition fieldDefinition = ImmutableCustomFieldDefinition.builder()
                .imhotepFieldName("custom")
                .name("my custom field")
                .customFieldId("1234")
                .multiValueFieldConfiguration(MultiValueFieldConfiguration.USERNAME)
                .build();
        builder.addCustomFieldColumns(fieldDefinition);
        final CustomFieldValue value = newCustomFieldValue(ImmutableList.of("user", "username"));
        EasyMock.expect(action.getCustomFieldValues()).andReturn(ImmutableMap.of(fieldDefinition, value)).anyTimes();
        verifyHeadersAndValues(
                ImmutableList.of("custom", "customusername"),
                ImmutableList.of("user", "username")
        );
    }

    @Test
    public void testVeryLongHistoryGetsTruncated() {
        final List<String> statusHistory = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            statusHistory.add("Closed");
            statusHistory.add("Reopened");
        }

        EasyMock.expect(action.getStatusHistory()).andReturn(statusHistory).once();

        final int maxLength = 35;
        final OutputFormatter outputFormatter = new OutputFormatter(OptionalInt.of(maxLength));
        final TSVSpecBuilder builder = new TSVSpecBuilder(outputFormatter, customFieldOutputter);
        builder.addStatusTimeColumns(Collections.emptyList());
        Assert.assertTrue(String.join("|", statusHistory).length() > maxLength);

        verifyHeadersAndValues(
                Collections.singletonList("statushistory*|"),
                Collections.singletonList("Closed|Reopened|Closed|<TRUNCATED>"),
                builder
        );
    }

    private CustomFieldValue newCustomFieldValue(final List<String> values) {
        final CustomFieldValue value = createNiceMock(CustomFieldValue.class);
        EasyMock.expect(customFieldOutputter.getValues(value)).andReturn(values).anyTimes();
        return value;
    }

    private void verifyHeadersAndValues(
            final List<String> expectedHeaders,
            final List<String> expectedValues
    ) {
        verifyHeadersAndValues(expectedHeaders, expectedValues, builder);
    }

    private void verifyHeadersAndValues(
            final List<String> expectedHeaders,
            final List<String> expectedValues,
            final TSVSpecBuilder builder
    ) {
        final List<TSVColumnSpec> specs = builder.build();
        final List<String> actualHeaders = specs.stream()
                .map(TSVColumnSpec::getHeader)
                .collect(Collectors.toList());
        Assert.assertThat(actualHeaders, is(equalTo(expectedHeaders)));
        replayAll();
        for (int i = 0; i < specs.size(); i++) {
            final TSVColumnSpec spec = specs.get(i);
            final String expectedValue = expectedValues.get(i);
            Assert.assertEquals(expectedValue, spec.getActionExtractor().apply(action));
        }
    }
}

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class TSVSpecBuilderTest extends EasyMockSupport {
    Action action;
    TSVSpecBuilder builder;

    @Before
    public void before() {
        action = createNiceMock(Action.class);
        builder = new TSVSpecBuilder();
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

    private CustomFieldValue newCustomFieldValue(final List<String> values) {
        final CustomFieldValue value = createNiceMock(CustomFieldValue.class);
        EasyMock.expect(value.getValues()).andReturn(values).anyTimes();
        return value;
    }

    private void verifyHeadersAndValues(final List<String> expectedHeaders, final List<String> expectedValues) {
        final List<TSVColumnSpec> specs = builder.build();
        final List<String> actualHeaders = specs.stream()
                .map(TSVColumnSpec::getHeader)
                .collect(Collectors.toList());
        Assert.assertEquals(expectedHeaders, actualHeaders);
        replayAll();
        for (int i = 0; i < specs.size(); i++) {
            final TSVColumnSpec spec = specs.get(i);
            final String expectedValue = expectedValues.get(i);
            Assert.assertEquals(expectedValue, spec.getActionExtractor().apply(action));
        }
    }
}

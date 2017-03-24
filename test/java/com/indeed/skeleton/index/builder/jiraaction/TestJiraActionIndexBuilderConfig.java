package com.indeed.skeleton.index.builder.jiraaction;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.beans.IntrospectionException;
import java.util.List;

public class TestJiraActionIndexBuilderConfig {
    @Test
    public void testSetters() throws IntrospectionException {
        final List<String> args = ImmutableList.<String>builder()
                .add("jiraUsernameIndexer")
                .add("jiraPasswordIndexer")
                .add("jiraBaseURL")
                .add("jiraFields")
                .add("jiraExpand")
                .add("jiraProject")
                .add("iuploadURL")
                .add("startDate")
                .add("endDate")
                .add("jiraBatchSize")
                .build();

        //JavaBeanAsserter.assertConstructorMatchesProperties(JiraActionIndexBuilderConfig.class, args);
    }
}

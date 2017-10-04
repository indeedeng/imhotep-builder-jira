package com.indeed.jiraactions;

import com.google.common.collect.ImmutableList;
import com.indeed.test.bean.JavaBeanAsserter;
import org.junit.Test;

import java.beans.IntrospectionException;
import java.util.List;

public class TestJiraActionIndexBuilderConfig {
    @Test
    public void testSetters() throws IntrospectionException {
        final List<String> args = ImmutableList.<String>builder()
                .add("jiraUsername")
                .add("jiraPassword")
                .add("jiraBaseURL")
                .add("jiraFields")
                .add("jiraExpand")
                .add("jiraProject")
                .add("excludedJiraProject")
                .add("iuploadURL")
                .add("iuploadUsername")
                .add("iuploadPassword")
                .add("startDate")
                .add("endDate")
                .add("jiraBatchSize")
                .add("indexName")
                .add("customFields")
                .build();

        JavaBeanAsserter.assertConstructorMatchesProperties(JiraActionsIndexBuilderConfig.class, args);
    }
}

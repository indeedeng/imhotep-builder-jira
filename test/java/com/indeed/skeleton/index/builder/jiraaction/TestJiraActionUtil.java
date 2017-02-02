package com.indeed.skeleton.index.builder.jiraaction;

import com.indeed.test.IndeedAsserts;
import org.junit.Test;

import java.beans.IntrospectionException;

public class TestJiraActionUtil {
    @Test
    public void testPrivateConstructor() throws IntrospectionException {
        IndeedAsserts.assertConstructorIsPrivate(JiraActionUtil.class);
    }
}

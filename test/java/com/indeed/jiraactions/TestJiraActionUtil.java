package com.indeed.jiraactions;

import com.indeed.test.IndeedAsserts;
import org.junit.Ignore;
import org.junit.Test;

import java.beans.IntrospectionException;

public class TestJiraActionUtil {
    @Test
    @Ignore
    public void testPrivateConstructor() throws IntrospectionException {
        IndeedAsserts.assertConstructorIsPrivate(JiraActionsUtil.class);
    }
}

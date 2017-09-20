package com.indeed.jiraactions;

import com.indeed.test.IndeedAsserts;
import org.junit.Ignore;
import org.junit.Test;

import java.beans.IntrospectionException;

public class TestJiraActionUtil {
    @Test
    @Ignore("Doesn't work on abstract classes")
    public void testPrivateConstructor() throws IntrospectionException {
        IndeedAsserts.assertConstructorIsPrivate(JiraActionsUtil.class);
    }
}

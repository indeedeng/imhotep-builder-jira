package com.indeed.jiraactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.jiraactions.api.response.issue.ImmutableUser;
import com.indeed.jiraactions.api.response.issue.User;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class ApiUserLookupServiceTest {
    @Test
    public void test() throws Exception {
        final JsonNode node = new ObjectMapper().readTree(new File("src/test/resources/example-user.json"));
        final User user = ApiUserLookupService.parseUser(node);
        final User expectedUser = ImmutableUser.builder()
                .displayName("John Doe")
                .name("johndoe")
                .key("johndoe")
                .addGroups("itsystems", "engineering", "jira-users", "product-managers")
                .build();
        Assert.assertEquals(expectedUser, user);
    }
}

package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment;

import com.indeed.skeleton.index.builder.jiraaction.JiraActionUtil;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.User;
import org.junit.Assert;
import org.junit.Test;

public class TestComment {
    @Test
    public void testComment() {
        final Comment comment = new Comment();
        Assert.assertFalse(comment.isValid());

        comment.id = "id";

        final User user = new User();
        comment.author = user;

        comment.body = "body";
        comment.created = JiraActionUtil.parseDateTime("2017-01-01");
        comment.updated = "2017-01-02";

        Assert.assertTrue(comment.isValid());
    }
}

package com.indeed.jiraactions.api.response.issue.fields.comment;

import com.indeed.jiraactions.JiraActionsUtil;
import com.indeed.jiraactions.api.response.issue.User;
import org.junit.Assert;
import org.junit.Test;

public class TestComment {
    @Test
    public void testComment() {
        final Comment comment = new Comment();
        Assert.assertFalse(comment.isValid());

        comment.id = "id";

        final User user = User.INVALID_USER;
        comment.author = user;

        comment.body = "body";
        comment.created = JiraActionsUtil.parseDateTime("2017-01-01");

        Assert.assertTrue(comment.isValid());
    }
}

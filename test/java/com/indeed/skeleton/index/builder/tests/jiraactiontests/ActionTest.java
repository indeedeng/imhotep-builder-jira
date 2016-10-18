package com.indeed.skeleton.index.builder.tests.jiraactiontests;

import com.indeed.skeleton.index.builder.jiraaction.Action;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.User;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.Comment;
import com.indeed.test.junit.Check;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;

import static org.easymock.EasyMock.anyObject;

/**
 * Created by soono on 9/2/16.
 */
public class ActionTest {
    Action prevAction;
    History history;
    User author;
    Comment comment;

    // default values
    final long prevActionIssueage = 100;
    final long prevActionTimeinstate = 50;
    final String prevActionTimestamp = "2016-09-02T01:01:00";
    final String historyCreated = "2016-09-02T01:01:10"; // diff with prevAction is 10s.
    final String commentCreated = "2016-09-02T01:01:10"; // diff with prevAction is 10s.
    final long timeDiffWithPrevAction = 10;

    @Before
    public void initialize() {
        prevAction = EasyMock.createNiceMock(Action.class);
        author = EasyMock.createNiceMock(User.class);

        // Set default values
        prevAction.action = "update";
        prevAction.issueage = prevActionIssueage;
        prevAction.timestamp = prevActionTimestamp;

        // For Update Action
        history = EasyMock.createNiceMock(History.class);
        history.author = author;
        history.created = historyCreated;

        // For Comment Action
        comment = EasyMock.createNiceMock(Comment.class);
        comment.author = author;
        comment.created = commentCreated;

    }

    //
    // No Test For Create Action's constructor .
    // Because the constructor has no logic.
    //

    //
    // Test Update Action (which is a constructor whose arguments are Action and History).
    //

    @Test
    public void testAction_update_action() throws ParseException {
        EasyMock.replay(history);

        final Action action = new Action(prevAction, history);
        Check.checkTrue("update".equals(action.action));
    }

    @Test
    public void testAction_update_actor() throws ParseException {
        EasyMock.replay(history);

        final String actor = "Test Actor";
        author.displayName = actor;

        final Action action = new Action(prevAction, history);
        Check.checkTrue(action.actor.equals(actor));
    }

    @Test
    public void testAction_update_assignee_whenAssigneeChanged() throws ParseException {
        final String assignee = "Test Assignee";
        EasyMock.expect(history.itemExist("assignee")).andReturn(true);
        EasyMock.expect(history.getItemLastValue("assignee")).andReturn(assignee);
        EasyMock.replay(history);


        final Action action = new Action(prevAction, history);
        Check.checkTrue(action.assignee.equals(assignee));
    }

    @Test
    public void testAction_update_assignee_whenAssigneeNotChanged() throws ParseException {
        final String assignee = "Test Assignee";
        EasyMock.expect(history.itemExist("assignee")).andReturn(false);
        EasyMock.replay(history);
        prevAction.assignee = assignee;

        final Action action = new Action(prevAction, history);
        Check.checkTrue(action.assignee.equals(assignee));
    }

    @Test
    public void testAction_update_issueage() throws ParseException {
        EasyMock.replay(history);

        final Action action = new Action(prevAction, history);
        Check.checkEquals(prevActionIssueage + timeDiffWithPrevAction, action.issueage);
    }

    @Test
    public void testAction_update_timeinstate() throws ParseException {
        EasyMock.replay(history);

        final Action action = new Action(prevAction, history);
        Check.checkEquals(timeDiffWithPrevAction, action.issueage);
    }

    //
    // Test Comment Action (which is a constructor whose arguments are Action and Comment).
    //

    @Test
    public void testAction_comment_issueage() throws ParseException {
        final Action action = new Action(prevAction, comment);
        Check.checkEquals(prevActionTimeinstate + timeDiffWithPrevAction, action.issueage);
    }

    @Test
    public void testAction_comment_timeinstate_afterUpdate() throws ParseException {
        prevAction.action = "update";
        final Action action = new Action(prevAction, comment);
        Check.checkEquals(timeDiffWithPrevAction, action.issueage);
    }

    @Test
    public void testAction_comment_timeinstate_afterComment() throws ParseException {
        prevAction.action = "comment";
        prevAction.timeinstate = prevActionTimeinstate;
        final Action action = new Action(prevAction, comment);
        Check.checkEquals(prevActionTimeinstate + timeDiffWithPrevAction, action.issueage);
    }
}

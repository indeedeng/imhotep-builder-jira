package com.indeed.skeleton.index.builder.jiraaction;

import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.User;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.Item;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.Comment;
import com.indeed.test.junit.Check;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;

/**
 * @author soono
 */
public class ActionTest {
    private Action prevAction;
    private History history;
    private History history2;
    private User author;
    private Comment comment;

    // default values
    private final long prevActionTimeinstate = 50;
    private static final String prevActionTimestamp = "2016-09-02T01:00:00";
    private static final String historyCreated = "2016-09-02T01:00:10";
    private static final String historyCreated2 = "2016-09-02T01:00:20"; // diff with prevAction is 10s.
    private static final String commentCreated = "2016-09-02T01:00:30"; // diff with prevAction is 10s.
    private static final long timeDiffWithPrevAction = 10;

    @Before
    public void initialize() {
        prevAction = EasyMock.createNiceMock(Action.class);
        author = EasyMock.createNiceMock(User.class);

        // Set default values
        prevAction.action = "create";
        prevAction.timestamp = prevActionTimestamp;
        prevAction.prevstatus = "";
        prevAction.status = "Pending Triage";

        history = new History();
        history.author = author;
        history.created = historyCreated;
        final Item historyItem = new Item();
        historyItem.setField("verifier");
        historyItem.fromString = "";
        historyItem.toString = "Test User";
        history.items = new Item[] { historyItem };

        // For Update Action
        history2 = new History();
        history2.author = author;
        history2.created = historyCreated2;
        history2.items = new Item[] { };

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
        final Action action = new Action(prevAction, history);
        Check.checkTrue("update".equals(action.action));
    }

    @Test
    public void testAction_update_actor() throws ParseException {
        final String actor = "Test Actor";
        author.displayName = actor;

        final Action action = new Action(prevAction, history);
        Check.checkTrue(action.actor.equals(actor));
    }

    @Test
    public void testAction_update_assignee_whenAssigneeChanged() throws ParseException {
        final Item item = new Item();
        item.setField("assignee");
        item.fromString = "old";
        item.toString = "new";
        history2.items = new Item[] { item };

        final Action action = new Action(prevAction, history2);
        Check.checkTrue(action.assignee.equals(item.toString));
        Check.checkTrue(action.fieldschanged.contains(item.field));
    }

    @Test
    public void testAction_update_assignee_whenAssigneeNotChanged() throws ParseException {
        final String assignee = "Test Assignee";
        prevAction.assignee = assignee;

        final Action action = new Action(prevAction, history);
        Check.checkTrue(action.assignee.equals(assignee));
    }

    @Test
    public void testAction_updatenotstate_timing() throws ParseException {
        final Action action = new Action(prevAction, history);
        Check.checkEquals(timeDiffWithPrevAction, action.issueage);
        Check.checkEquals(timeDiffWithPrevAction, action.timeinstate);
        Check.checkEquals(timeDiffWithPrevAction, action.timesinceaction);
    }

    @Test
    public void testAction_update_timeinstate() throws ParseException {
        final Action action = new Action(prevAction, history);

        final Action action2 = new Action(action, history2);
        Assert.assertEquals(timeDiffWithPrevAction*2, action2.issueage);
        Assert.assertEquals(timeDiffWithPrevAction*2, action2.timeinstate);
        Assert.assertEquals(timeDiffWithPrevAction, action2.timesinceaction);
    }

    //
    // Test Comment Action (which is a constructor whose arguments are Action and Comment).
    //

    @Test
    public void testAction_comment_issueage() throws ParseException {
        final Action action = new Action(prevAction, history);

        final Action action2 = new Action(action, comment);
        Assert.assertEquals(timeDiffWithPrevAction*3, action2.issueage);
        Assert.assertEquals(timeDiffWithPrevAction*3, action2.timeinstate);
        Assert.assertEquals(timeDiffWithPrevAction*2, action2.timesinceaction);
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

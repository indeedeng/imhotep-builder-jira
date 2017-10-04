package com.indeed.jiraactions;

import com.indeed.jiraactions.api.customfields.CustomFieldApiParser;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;
import com.indeed.jiraactions.api.response.issue.fields.comment.Comment;
import com.indeed.test.junit.Check;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
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
    private static final long prevActionTimeinstate = 50;
    private static final DateTime prevActionTimestamp = JiraActionsUtil.parseDateTime("2016-09-02T01:00:00");
    private static final DateTime historyCreated = JiraActionsUtil.parseDateTime("2016-09-02T01:00:10");
    private static final DateTime historyCreated2 = JiraActionsUtil.parseDateTime("2016-09-02T01:00:20"); // diff with prevAction is 10s.
    private static final DateTime commentCreated = JiraActionsUtil.parseDateTime("2016-09-02T01:00:30"); // diff with prevAction is 10s.
    private static final long timeDiffWithPrevAction = 10;

    private final UserLookupService userLookupService = new FriendlyUserLookupService();
    private ActionFactory actionFactory;

    @Before
    public void initialize() {
        final JiraActionsIndexBuilderConfig config = EasyMock.createNiceMock(JiraActionsIndexBuilderConfig.class);
        EasyMock.expect(config.getCustomFields()).andReturn(new CustomFieldDefinition[0]).anyTimes();
        EasyMock.replay(config);

        actionFactory = new ActionFactory(userLookupService, new CustomFieldApiParser(userLookupService), config);

        author = new User();
        author.displayName = "Author";
        author.name = "author";

        final Action defaultAction = ImmutableProxy.createProxy(Action.class);

        // Set default values
        prevAction = ImmutableAction.builder()
                .from(defaultAction)
                .action("create")
                .timestamp(prevActionTimestamp)
                .prevstatus("")
                .status("Pending Triage")
                .build();

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
        comment = new Comment();
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
    public void testAction_update_action() throws ParseException, IOException {
        final Action action = actionFactory.update(prevAction, history);
        Check.checkTrue("update".equals(action.getAction()));
    }

    @Test
    public void testAction_update_actor() throws ParseException, IOException {
        final String actor = "Test Actor";
        author.displayName = actor;

        final Action action = actionFactory.update(prevAction, history);
        Check.checkTrue(action.getActor().equals(actor));
    }

    @Test
    public void testAction_update_assignee_whenAssigneeChanged() throws ParseException, IOException {
        final Item item = new Item();
        item.setField("assignee");
        item.fromString = "old";
        item.toString = "new";
        history2.items = new Item[] { item };

        final Action action = actionFactory.update(prevAction, history2);
        Check.checkTrue(action.getAssignee().equals(item.toString));
        Check.checkTrue(action.getFieldschanged().contains(item.field));
    }

    @Test
    public void testAction_update_assignee_whenAssigneeNotChanged() throws ParseException, IOException {
        final String assignee = "Test Assignee";
        final Action newPrevAction = ImmutableAction.builder().from(prevAction).assignee(assignee).build();

        final Action action = actionFactory.update(newPrevAction, history);
        Check.checkTrue(action.getAssignee().equals(assignee));
    }

    @Test
    public void testAction_updatenotstate_timing() throws ParseException, IOException {
        final Action action = actionFactory.update(prevAction, history);
        Check.checkEquals(timeDiffWithPrevAction, action.getIssueage());
        Check.checkEquals(timeDiffWithPrevAction, action.getTimeinstate());
        Check.checkEquals(timeDiffWithPrevAction, action.getTimesinceaction());
    }

    @Test
    public void testAction_update_timeinstate() throws ParseException, IOException {
        final Action action = actionFactory.update(prevAction, history);

        final Action action2 = actionFactory.update(action, history2);
        Check.checkEquals(timeDiffWithPrevAction*2, action2.getIssueage());
        Check.checkEquals(timeDiffWithPrevAction*2, action2.getTimeinstate());
        Check.checkEquals(timeDiffWithPrevAction, action2.getTimesinceaction());
    }

    //
    // Test Comment Action (which is a constructor whose arguments are Action and Comment).
    //

    @Test
    public void testAction_comment_issueage() throws ParseException, IOException {
        final Action action = actionFactory.update(prevAction, history);

        final Action action2 = actionFactory.comment(action, comment);
        Check.checkEquals(timeDiffWithPrevAction*3, action2.getIssueage());
        Check.checkEquals(timeDiffWithPrevAction*3, action2.getTimeinstate());
        Check.checkEquals(timeDiffWithPrevAction*2, action2.getTimesinceaction());
    }

    @Test
    public void testAction_comment_timeinstate_afterUpdate() throws ParseException {
        final Action newPrevAction = ImmutableAction.builder().from(prevAction).action("update").build();

        final Action action = actionFactory.comment(newPrevAction, comment);
        Check.checkEquals(timeDiffWithPrevAction, action.getIssueage());
    }

    @Test
    public void testAction_comment_timeinstate_afterComment() throws ParseException {
        final Action newPrevAction = ImmutableAction.builder().from(prevAction)
                .action("comment")
                .timeinstate(prevActionTimeinstate)
                .build();
        final Action action = actionFactory.comment(newPrevAction, comment);
        Check.checkEquals(prevActionTimeinstate + timeDiffWithPrevAction, action.getIssueage());
    }

    @Test
    public void testChangeStatusAndChangeBack() throws IOException {
        final Action newPrevAction = ImmutableAction.builder().from(prevAction)
                .action("update")
                .fieldschanged("status")
                .prevstatus("On Backlog")
                .status("Accepted")
                .timeinstate(100)
                .build();

        final Item item = new Item();
        item.setField("status");
        item.fromString = "Accepted";
        item.toString = "On Backlog";
        history2.items = new Item[] { item };

        final Action action = actionFactory.update(newPrevAction, history2);
        Check.checkEquals(history2.created.getMillis() - prevAction.getTimestamp().getMillis(), action.getTimeinstate());
    }
}

package com.indeed.skeleton.index.builder.tests.jiraactiontests;

import com.indeed.skeleton.index.builder.jiraaction.Action;
import com.indeed.skeleton.index.builder.jiraaction.ActionsBuilder;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.User;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.ChangeLog;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.Field;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.Comment;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.CommentCollection;
import com.indeed.test.junit.Check;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by soono on 9/6/16.
 */
public class ActionsBuilderTest {
    Issue issue;
    private static final String yesterday;
    private static final String dayBeforeYesterday;

    static {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        yesterday = parseDateToString(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, -1);
        dayBeforeYesterday = parseDateToString(cal.getTime());
    }

    @Before
    public void initialize() {
        issue = EasyMock.createNiceMock(Issue.class);

        final Field field = EasyMock.createNiceMock(Field.class);
        issue.fields = field;

        final ChangeLog changeLog = EasyMock.createNiceMock(ChangeLog.class);
        issue.changelog = changeLog;
        final History[] histories = new History[]{};
        issue.changelog.histories = histories;

        final CommentCollection comment = new CommentCollection();
        issue.fields.comment = comment;
        final Comment[] comments = new Comment[]{};
        issue.fields.comment.comments = comments;

        EasyMock.replay(issue);
        EasyMock.replay(field);
        EasyMock.replay(changeLog);
    }

    @Test
    public void testBuildActions_newIssueHasCreateAction() throws Exception {
        setIssueNew();
        setExpectationsForCreateAction();

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue);
        actionsBuilder.buildActions();

        final Action createAction = new Action(issue);
        Assert.assertTrue(true);
        Check.checkTrue(false);
//        Check.checkTrue(actionsBuilder.actions.get(0).equals(createAction));
    }

    @Test
    public void testBuildActions_oldIssueDoesNotHaveCreateAction() throws Exception {
        setIssueOld();
        setExpectationsForCreateAction();

        setHistoryNew();

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue);
        actionsBuilder.buildActions();

        final Action createAction = new Action(issue);
        Check.checkFalse(actionsBuilder.actions.get(0).equals(createAction));
    }

    @Test
    public void testBuildActions_setNewUpdate() throws Exception {
        setIssueNew();
        setExpectationsForCreateAction();

        setHistoryNew();

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue);
        actionsBuilder.buildActions();

        boolean containsUpdate = false;
        for (final Action action : actionsBuilder.actions) {
            if (action.action == "update") containsUpdate = true;
        }
        Check.checkTrue(containsUpdate);
    }

    @Test
    public void testBuildActions_doesNotSetOldUpdate() throws Exception {
        setIssueNew();
        setExpectationsForCreateAction();

        setHistoryOld();

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue);
        actionsBuilder.buildActions();

        boolean containsUpdate = false;
        for (final Action action : actionsBuilder.actions) {
            if (action.action == "update") containsUpdate = true;
        }
        Check.checkFalse(containsUpdate);
    }

    @Test
    public void testBuildActions_setNewComments() throws Exception {
        setIssueNew();
        setExpectationsForCreateAction();

        setCommentNew();

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue);
        actionsBuilder.buildActions();

        boolean containsComment = false;
        for (final Action action : actionsBuilder.actions) {
            if (action.action == "update") containsComment = true;
        }
        Check.checkTrue(containsComment);
    }

    @Test
    public void testBuildActions_doesNotSetOldComments() throws Exception {
        setIssueNew();
        setExpectationsForCreateAction();

        setCommentOld();

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue);
        actionsBuilder.buildActions();

        boolean containsComment = false;
        for (final Action action : actionsBuilder.actions) {
            if (action.action == "update") containsComment = true;
        }
        Check.checkFalse(containsComment);
    }

    private void setIssueNew() throws ParseException {
        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        issue.fields.created = parseDateToString(yesterday.getTime());
    }


    private void setIssueOld() throws ParseException {
        final Calendar dayBeforeYesterday = Calendar.getInstance();
        dayBeforeYesterday.add(Calendar.DAY_OF_YEAR, -2);
        issue.fields.created = parseDateToString(dayBeforeYesterday.getTime());
    }

    private void setExpectationsForCreateAction() {
        final User issueCreator = EasyMock.createNiceMock(User.class);
        issueCreator.displayName = "Test issueCreator";
        issue.fields.creator = issueCreator;
    }

    private void setHistoryNew() {
        final History history = EasyMock.createNiceMock(History.class);
        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        history.created = parseDateToString(yesterday.getTime());

        final User historyAuthor = EasyMock.createNiceMock(User.class);
        history.author = historyAuthor;
        EasyMock.replay(history);

        final History[] histories = new History[] { history };
        issue.changelog.histories = histories;
    }

    private void setHistoryOld() {
        final History history = EasyMock.createNiceMock(History.class);
        final Calendar dayBeforeYesterday = Calendar.getInstance();
        dayBeforeYesterday.add(Calendar.DAY_OF_YEAR, -2);
        history.created = parseDateToString(dayBeforeYesterday.getTime());

        final User historyAuthor = EasyMock.createNiceMock(User.class);
        history.author = historyAuthor;
        EasyMock.replay(history);

        final History[] histories = new History[] { history };
        issue.changelog.histories = histories;
    }

    private void setCommentNew() {
        final Comment comment = EasyMock.createNiceMock(Comment.class);
        comment.created = yesterday;

        final User commentAuthor = EasyMock.createNiceMock(User.class);
        comment.author = commentAuthor;
        EasyMock.replay(comment);

        final Comment[] comments = new Comment[] { comment };
        issue.fields.comment.comments = comments;
    }

    private void setCommentOld() {
        final Comment comment = EasyMock.createNiceMock(Comment.class);
        comment.created = dayBeforeYesterday;

        final User commentAuthor = EasyMock.createNiceMock(User.class);
        comment.author = commentAuthor;
        EasyMock.replay(comment);

        final Comment[] comments = new Comment[] { comment };
        issue.fields.comment.comments = comments;

    }

    private static String parseDateToString(final Date date) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        final String dateString = dateFormat.format(date).replace(' ', 'T');
        return dateString;
    }
}

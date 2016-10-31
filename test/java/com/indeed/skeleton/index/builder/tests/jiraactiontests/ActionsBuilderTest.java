package com.indeed.skeleton.index.builder.tests.jiraactiontests;

import com.indeed.skeleton.index.builder.jiraaction.Action;
import com.indeed.skeleton.index.builder.jiraaction.ActionsBuilder;
import com.indeed.skeleton.index.builder.jiraaction.JiraActionUtil;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.User;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.ChangeLog;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.Field;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.Comment;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.CommentCollection;
import com.indeed.test.junit.Check;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;

/**
 * @author soono
 * @author kbinswanger
 */
public class ActionsBuilderTest {
    private Issue issue;
    private DateTime withinRange;
    private DateTime withinRange2;
    private DateTime beforeRange;

    private DateTime startDate;
    private DateTime endDate;

    @Before
    public void initialize() throws ParseException {
        startDate = JiraActionUtil.parseDateTime("2016-08-01 00:00:00");
        endDate = JiraActionUtil.parseDateTime("2016-08-02 00:00:00");

        withinRange = JiraActionUtil.parseDateTime("2016-08-01 01:00:00");
        withinRange2 = JiraActionUtil.parseDateTime("2016-08-01 02:00:00");
        beforeRange = JiraActionUtil.parseDateTime("2016-07-30 11:59:59");

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

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
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

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
        actionsBuilder.buildActions();

        Check.checkFalse(actionsBuilder.actions.get(0).action.equals("create"));
    }

    @Test
    public void testBuildActions_setNewUpdate() throws Exception {
        setIssueNew();
        setExpectationsForCreateAction();

        setHistoryNew();

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
        actionsBuilder.buildActions();

        boolean containsUpdate = false;
        for (final Action action : actionsBuilder.actions) {
            if ("update".equals(action.action)) {
                containsUpdate = true;
            }
        }
        Check.checkTrue(containsUpdate);
    }

    @Test
    public void testBuildActions_doesNotSetOldUpdate() throws Exception {
        setIssueNew();
        setExpectationsForCreateAction();

        setHistoryOld();

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
        actionsBuilder.buildActions();

        boolean containsUpdate = false;
        for (final Action action : actionsBuilder.actions) {
            if ("update".equals(action.action)) {
                containsUpdate = true;
            }
        }
        Check.checkFalse(containsUpdate);
    }

    @Test
    public void testBuildActions_setNewComments() throws Exception {
        setIssueNew();
        setExpectationsForCreateAction();

        setCommentNew();

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
        actionsBuilder.buildActions();

        boolean containsComment = false;
        for (final Action action : actionsBuilder.actions) {
            if ("update".equals(action.action)) {
                containsComment = true;
            }
        }
        Check.checkTrue(containsComment);
    }

    @Test
    public void testBuildActions_doesNotSetOldComments() throws Exception {
        setIssueNew();
        setExpectationsForCreateAction();

        setCommentOld();

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
        actionsBuilder.buildActions();

        boolean containsComment = false;
        for (final Action action : actionsBuilder.actions) {
            if ("update".equals(action.action)) {
                containsComment = true;
            }
        }
        Check.checkFalse(containsComment);
    }

    private void setIssueNew() throws ParseException {
        issue.fields.created = parseDateToString(withinRange);
    }


    private void setIssueOld() throws ParseException {
        issue.fields.created = parseDateToString(beforeRange);
    }

    private void setExpectationsForCreateAction() {
        final User issueCreator = EasyMock.createNiceMock(User.class);
        issueCreator.displayName = "Test issueCreator";
        issue.fields.creator = issueCreator;
    }

    private void setHistoryNew() {
        final History history = EasyMock.createNiceMock(History.class);
        history.created = parseDateToString(withinRange2);

        final User historyAuthor = EasyMock.createNiceMock(User.class);
        history.author = historyAuthor;
        EasyMock.replay(history);

        final History[] histories = new History[] { history };
        issue.changelog.histories = histories;
    }

    private void setHistoryOld() {
        final History history = EasyMock.createNiceMock(History.class);
        history.created = parseDateToString(beforeRange);

        final User historyAuthor = EasyMock.createNiceMock(User.class);
        history.author = historyAuthor;
        EasyMock.replay(history);

        final History[] histories = new History[] { history };
        issue.changelog.histories = histories;
    }

    private void setCommentNew() {
        final Comment comment = EasyMock.createNiceMock(Comment.class);
        comment.created = parseDateToString(withinRange);

        final User commentAuthor = EasyMock.createNiceMock(User.class);
        comment.author = commentAuthor;
        EasyMock.replay(comment);

        final Comment[] comments = new Comment[] { comment };
        issue.fields.comment.comments = comments;
    }

    private void setCommentOld() {
        final Comment comment = EasyMock.createNiceMock(Comment.class);
        comment.created = parseDateToString(beforeRange);

        final User commentAuthor = EasyMock.createNiceMock(User.class);
        comment.author = commentAuthor;
        EasyMock.replay(comment);

        final Comment[] comments = new Comment[] { comment };
        issue.fields.comment.comments = comments;

    }

    private static String parseDateToString(final DateTime date) {
        return date.toString(JiraActionUtil.DATE_TIME_FORMATTER);
    }
}

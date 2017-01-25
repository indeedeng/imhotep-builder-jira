package com.indeed.skeleton.index.builder.jiraaction;

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
    private DateTime withinRangeComment;
    private DateTime beforeRange;
    private DateTime beforeRangeComment;
    private DateTime afterRange;
    private DateTime afterRangeComment;

    private DateTime startDate;
    private DateTime endDate;

    @Before
    public void initialize() throws ParseException {
        startDate = JiraActionUtil.parseDateTime("2016-08-01 00:00:00");
        endDate = JiraActionUtil.parseDateTime("2016-08-02 00:00:00");

        withinRange = JiraActionUtil.parseDateTime("2016-08-01 01:00:00");
        withinRangeComment = JiraActionUtil.parseDateTime("2016-08-01 02:00:00");
        beforeRange = JiraActionUtil.parseDateTime("2016-07-30 11:00:00");
        beforeRangeComment = JiraActionUtil.parseDateTime("2016-07-30 11:59:59");
        afterRange = JiraActionUtil.parseDateTime("2016-08-02 01:00:00");
        afterRangeComment = JiraActionUtil.parseDateTime("2016-08-03");

        issue = EasyMock.createNiceMock(Issue.class);

        final Field field = EasyMock.createNiceMock(Field.class);
        issue.fields = field;

        final ChangeLog changeLog = EasyMock.createNiceMock(ChangeLog.class);
        issue.changelog = changeLog;
        final History[] histories = {};
        issue.changelog.histories = histories;

        final CommentCollection comment = new CommentCollection();
        issue.fields.comment = comment;
        final Comment[] comments = {};
        issue.fields.comment.comments = comments;

        EasyMock.replay(issue);
        EasyMock.replay(field);
        EasyMock.replay(changeLog);
    }

    @Test
    public void testBuildActions_newIssueHasCreateAction() throws Exception {
        setIssueOkay();
        setExpectationsForCreateAction();

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
        actionsBuilder.buildActions();

        Assert.assertTrue(true);
        Check.checkTrue(false);
        Check.checkTrue("create".equals(actionsBuilder.actions.get(0).action));
    }

    @Test
    public void testBuildActions_oldIssueDoesNotHaveCreateAction() throws Exception {
        setIssueOld();
        setExpectationsForCreateAction();

        setHistoryOkay();

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
        actionsBuilder.buildActions();

        Check.checkFalse("create".equals(actionsBuilder.actions.get(0).action));
    }

    @Test
    public void testBuildActions_setNewUpdate() throws Exception {
        setIssueOkay();
        setExpectationsForCreateAction();

        setHistoryOkay();

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
        setIssueOkay();
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
        setIssueOkay();
        setExpectationsForCreateAction();

        setCommentOkay();

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
        setIssueOkay();
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

    private void setIssueNew() {
        issue.fields.created = parseDateToString(afterRange);
    }
    
    private void setIssueOkay() {
        issue.fields.created = parseDateToString(withinRange);
    }


    private void setIssueOld() {
        issue.fields.created = parseDateToString(beforeRange);
    }

    private void setExpectationsForCreateAction() {
        final User issueCreator = EasyMock.createNiceMock(User.class);
        issueCreator.displayName = "Test issueCreator";
        issue.fields.creator = issueCreator;
    }

    private void createHistory(final DateTime created) {
        final History history = EasyMock.createNiceMock(History.class);
        history.created = parseDateToString(created);

        final User historyAuthor = EasyMock.createNiceMock(User.class);
        history.author = historyAuthor;
        EasyMock.replay(history);

        final History[] histories = { history };
        issue.changelog.histories = histories;        
    }
    private void setHistoryNew() {
        createHistory(afterRange);
    }
    
    private void setHistoryOkay() {
        createHistory(withinRange);
    }

    private void setHistoryOld() {
        createHistory(beforeRange);
    }

    private void createComment(final DateTime created) {
        final Comment comment = EasyMock.createNiceMock(Comment.class);
        comment.created = parseDateToString(created);

        final User commentAuthor = EasyMock.createNiceMock(User.class);
        comment.author = commentAuthor;
        EasyMock.replay(comment);

        final Comment[] comments = { comment };
        issue.fields.comment.comments = comments;
    }

    private void setCommentNew() {
        createComment(afterRangeComment);
    }

    private void setCommentOkay() {
        createComment(withinRangeComment);
    }

    private void setCommentOld() {
        createComment(beforeRangeComment);
    }

    private static String parseDateToString(final DateTime date) {
        return date.toString(JiraActionUtil.DATE_TIME_FORMATTER);
    }
}

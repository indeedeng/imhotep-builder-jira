package com.indeed.skeleton.index.builder.jiraaction;

import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.User;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.ChangeLog;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.Item;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.Field;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.Comment;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.CommentCollection;
import com.indeed.test.junit.Check;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author soono
 * @author kbinswanger
 */
public class ActionsBuilderTest {
    private Issue issue;
    private static final DateTime startDate = JiraActionUtil.parseDateTime("2016-08-01 00:00:00");
    private static final DateTime endDate = JiraActionUtil.parseDateTime("2016-08-07 00:00:00");

    @Before
    public void initialize() throws ParseException {
        issue = new Issue();
        issue.fields = new Field();

        final User issueCreator = new User();
        issueCreator.displayName = "Test issueCreator";
        issue.fields.creator = issueCreator;

        final ChangeLog changeLog = new ChangeLog();
        issue.changelog = changeLog;
        final History[] histories = {};
        issue.changelog.histories = histories;

        final CommentCollection comment = new CommentCollection();
        issue.fields.comment = comment;
        final Comment[] comments = {};
        issue.fields.comment.comments = comments;
    }

    @Test
    public void testBuildActions_newIssueHasCreateAction() throws Exception {
        final DateTime issueDate = startDate.plusDays(1);
        setCreationDate(issueDate);

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
        final List<Action> actions = actionsBuilder.buildActions();

        Check.checkTrue("create".equals(actions.get(0).action));
    }

    @Test
    public void testBuildActions_oldIssueDoesNotHaveCreateAction() throws Exception {
        final DateTime issueDate = startDate.minusDays(7);
        setCreationDate(issueDate);

        final DateTime updateDate = startDate.plusDays(1);
        createHistory(updateDate);

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
        final List<Action> actions = actionsBuilder.buildActions();

        Check.checkFalse("create".equals(actions.get(0).action));
    }

    @Test
    public void testBuildActions_setNewUpdate() throws Exception {
        final DateTime issueDate = startDate.plusDays(1);
        setCreationDate(issueDate);

        final DateTime updateDate = issueDate.plusDays(1);
        createHistory(updateDate);

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
        final List<Action> actions = actionsBuilder.buildActions();

        boolean containsUpdate = false;
        for (final Action action : actions) {
            if ("update".equals(action.action)) {
                containsUpdate = true;
            }
        }
        Check.checkTrue(containsUpdate);
    }

    @Test
    public void testBuildActions_doesNotSetOldUpdate() throws Exception {
        final DateTime issueDate = startDate.minusDays(7);
        setCreationDate(issueDate);

        final DateTime tooOldUpdateDate = issueDate.plusDays(1);
        createHistory(tooOldUpdateDate);

        final DateTime tooNewUpdateDate = endDate.plusDays(1);
        createHistory(tooNewUpdateDate);

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
        final List<Action> actions = actionsBuilder.buildActions();

        boolean containsUpdate = false;
        for (final Action action : actions) {
            if ("update".equals(action.action)) {
                containsUpdate = true;
            }
        }
        Check.checkFalse(containsUpdate);
    }

    @Test
    public void testBuildActions_setNewComments() throws Exception {
        final DateTime issueDate = startDate.plusDays(1);
        setCreationDate(issueDate);

        final DateTime commentDate = issueDate.plusDays(1);
        createComment(commentDate);

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
        final List<Action> actions = actionsBuilder.buildActions();

        boolean containsComment = false;
        for (final Action action : actions) {
            if ("comment".equals(action.action)) {
                containsComment = true;
            }
        }
        Check.checkTrue(containsComment);
    }

    @Test
    public void testBuildActions_doesNotSetOldComments() throws Exception {
        final DateTime issueDate = startDate.minusDays(7);
        setCreationDate(issueDate);

        final DateTime tooOldUpdateDate = issueDate.plusDays(1);
        createComment(tooOldUpdateDate);

        final DateTime tooNewUpdateDate = endDate.plusDays(1);
        createComment(tooNewUpdateDate);

        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
        final List<Action> actions = actionsBuilder.buildActions();

        boolean containsComment = false;
        for (final Action action : actions) {
            if ("comment".equals(action.action)) {
                containsComment = true;
            }
        }
        Check.checkFalse(containsComment);
    }

    private void createHistory(final DateTime created) {
        final History history = new History();
        history.items = new Item[0];

        history.created = created;

        final User historyAuthor = new User();
        history.author = historyAuthor;

        if(issue.changelog.histories == null) {
            issue.changelog.histories = new History[0];
        }

        final List<History> tempHistories = new ArrayList<>(Arrays.asList(issue.changelog.histories));
        tempHistories.add(history);
        final History[] histories = tempHistories.toArray(new History[tempHistories.size()]);
        issue.changelog.histories = histories;
    }

    private void createComment(final DateTime created) {
        final Comment comment = new Comment();
        comment.created = created;

        final User commentAuthor = new User();
        comment.author = commentAuthor;

        if(issue.fields.comment.comments == null) {
            issue.fields.comment.comments = new Comment[0];
        }

        final List<Comment> tempComments = new ArrayList<>(Arrays.asList(issue.fields.comment.comments));
        tempComments.add(comment);
        final Comment[] comments = tempComments.toArray(new Comment[tempComments.size()]);
        issue.fields.comment.comments = comments;
    }

    private void setCreationDate(final DateTime date) {
        issue.fields.created = date;
    }
}

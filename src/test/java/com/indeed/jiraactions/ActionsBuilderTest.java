package com.indeed.jiraactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.indeed.jiraactions.api.IssueAPIParser;
import com.indeed.jiraactions.api.customfields.CustomFieldApiParser;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.response.issue.ImmutableUser;
import com.indeed.jiraactions.api.response.issue.Issue;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.jiraactions.api.response.issue.changelog.ChangeLog;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;
import com.indeed.jiraactions.api.response.issue.fields.Field;
import com.indeed.jiraactions.api.response.issue.fields.comment.Comment;
import com.indeed.jiraactions.api.response.issue.fields.comment.CommentCollection;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;

public class ActionsBuilderTest {
    private Issue issue;
    private static final DateTime startDate = JiraActionsUtil.parseDateTime("2016-08-01 00:00:00");
    private static final DateTime endDate = JiraActionsUtil.parseDateTime("2016-08-07 00:00:00");
    private final UserLookupService userLookupService = new FriendlyUserLookupService();
    private ActionFactory actionFactory;

    @Before
    public void initialize() throws ParseException {
        final JiraActionsIndexBuilderConfig config = EasyMock.createNiceMock(JiraActionsIndexBuilderConfig.class);
        EasyMock.expect(config.getCustomFields()).andReturn(new CustomFieldDefinition[0]).anyTimes();
        EasyMock.replay(config);

        actionFactory = new ActionFactory(userLookupService, new CustomFieldApiParser(userLookupService), config);

        issue = new Issue();
        issue.key = "ABC-123";
        issue.fields = new Field();

        final User issueCreator = ImmutableUser.builder()
                .displayName("Test issueCreator")
                .name("test")
                .key("test key")
                .build();
        issue.fields.creator = issueCreator;

        final ChangeLog changeLog = new ChangeLog();
        issue.changelog = changeLog;
        final History[] histories = {};
        issue.changelog.histories = histories;

        final CommentCollection comment = new CommentCollection();
        issue.fields.comment = comment;
        final Comment[] comments = {};
        issue.fields.comment.comments = comments;

        issue.fields.summary = "Summary";
    }
    
    @Test
    public void testBuildActions_newIssueHasCreateAction() throws Exception {
        final DateTime issueDate = startDate.plusDays(1);
        setCreationDate(issueDate);

        final ActionsBuilder actionsBuilder = new ActionsBuilder(actionFactory, issue, startDate, endDate);
        final List<Action> actions = actionsBuilder.buildActions();

        Assert.assertTrue("create".equals(actions.get(0).getAction()));
    }

    @Test
    public void testBuildActions_setNewUpdate() throws Exception {
        final DateTime issueDate = startDate.plusDays(1);
        setCreationDate(issueDate);

        final DateTime updateDate = issueDate.plusDays(1);
        createHistory(updateDate);

        final ActionsBuilder actionsBuilder = new ActionsBuilder(actionFactory, issue, startDate, endDate);
        final List<Action> actions = actionsBuilder.buildActions();

        boolean containsUpdate = false;
        for (final Action action : actions) {
            if ("update".equals(action.getAction())) {
                containsUpdate = true;
            }
        }
        Assert.assertTrue(containsUpdate);
    }

    @Test
    public void testBuildActions_setNewComments() throws Exception {
        final DateTime issueDate = startDate.plusDays(1);
        setCreationDate(issueDate);

        final DateTime commentDate = issueDate.plusDays(1);
        createComment(commentDate);

        final ActionsBuilder actionsBuilder = new ActionsBuilder(actionFactory, issue, startDate, endDate);
        final List<Action> actions = actionsBuilder.buildActions();

        boolean containsComment = false;
        for (final Action action : actions) {
            if ("comment".equals(action.getAction())) {
                containsComment = true;
            }
        }
        Assert.assertTrue(containsComment);
    }

    @Test
    public void testBuildActions_addingComponents() throws IOException {
        // Custom JSON issue includes the addition and removal of components in various combinations,
        //  resulting in a final list of two.
        try (final InputStream stream = getClass().getResourceAsStream("/ENGPLANS-10.testBuildActions_addingComponents.json")) {
            Issue issue = parseIssue(stream);

            Action action = actionFactory.create(issue);
            for (final History history: issue.changelog.histories) {
                action = actionFactory.update(action, history);
            }

            Assert.assertThat(action.getComponents(), equalTo(ImmutableList.of("D", "R")));
            Assert.assertThat(action.getComponentsJoined(), equalTo("D|R"));
        }
    }

    @Test
    public void testBuildActions_removingComponents() throws IOException {
        // Custom JSON issue includes the addition and removal of components in various combinations,
        //  resulting in an empty final list.
        try (final InputStream stream = getClass().getResourceAsStream("/ENGPLANS-10.testBuildActions_removingComponents.json")) {
            Issue issue = parseIssue(stream);

            Action action = actionFactory.create(issue);
            for (final History history: issue.changelog.histories) {
                action = actionFactory.update(action, history);
            }

            Assert.assertThat(action.getComponents(), equalTo(ImmutableList.of()));
        }
    }

    private Issue parseIssue(final InputStream stream) throws IOException {
        Assert.assertNotNull(stream);
        final JsonNode node = new ObjectMapper().readTree(stream);
        return IssueAPIParser.getObject(node);
    }

    private void createHistory(final DateTime created) {
        final History history = new History();
        history.items = new Item[0];

        history.created = created;

        final User historyAuthor = ImmutableUser.builder()
                .displayName("AuthorDisplayName")
                .name("name")
                .key("author key")
                .build();
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

        final User commentAuthor = ImmutableUser.builder()
                .displayName("commentDisplayName")
                .name("commentName")
                .key("comment key")
                .build();
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

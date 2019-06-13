package com.indeed.jiraactions;

import com.indeed.jiraactions.api.response.issue.Issue;
import com.indeed.jiraactions.api.response.issue.changelog.ChangeLog;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.fields.comment.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ActionsBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ActionsBuilder.class);

    private final Issue issue;
    private final DateTime startDate;
    private final DateTime endDate;
    private final ActionFactory actionFactory;
    private final List<History> histories;
    private final List<Comment> comments;
    private Action action;


    public ActionsBuilder(final ActionFactory actionFactory, final Issue issue, final DateTime startDate, final DateTime endDate) {
        this.actionFactory = actionFactory;
        this.issue = issue;
        this.startDate = startDate;
        this.endDate = endDate;

        action = null;
        histories = sortLatestHistories(issue);
        comments = sortLatestComments(issue);
    }

    @Nonnull
    public Action buildActions() throws IOException {
        setCreateAction();
        compare();
        setActionToCurrent();
        return action;
    }

    //
    // For Create Action
    //

    private void setCreateAction() throws IOException {
        action = actionFactory.create(issue);
    }

    //
    // For Update Action
    //

    private void setUpdateActions(int index) {
        action = actionFactory.update(action, histories.get(index));
    }

    //
    // For Comment Action
    //

    private void setCommentActions(int index) {
        action = actionFactory.comment(action, comments.get(index));
    }

    //
    // Updating Action to Given Date
    //

    private void setActionToCurrent() {
        action = actionFactory.toCurrent(action);
    }

    private void compare() {
        int historyIndex = 0;
        int commentIndex = 0;
        while (true) {
            if (historyIndex >= histories.size() || commentIndex >= comments.size()) {
                if (commentIndex >= comments.size()) {
                    if (historyIndex >= histories.size()) {
                        break;
                    } else {
                        setUpdateActions(historyIndex);
                        historyIndex++;
                    }
                } else {
                    setCommentActions(commentIndex);
                    commentIndex++;
                }
            } else {
                if (histories.get(historyIndex).created.isBefore(comments.get(commentIndex).created)) {
                    setUpdateActions(historyIndex);
                    historyIndex++;
                } else {
                    setCommentActions(commentIndex);
                    commentIndex++;
                }
            }
        }
    }

    private List<History> sortLatestHistories(final Issue issue) {
        issue.changelog.sortHistories();
        final History[] histories = issue.changelog.histories;
        final List<History> listHistories = new ArrayList<>(histories.length);
        listHistories.addAll(Arrays.asList(histories));
        return listHistories.stream().filter(a -> a.isBefore(startDate)).collect(Collectors.toList());
    }

    private List<Comment> sortLatestComments(final Issue issue) {
        issue.fields.comment.sortComments();
        final Comment[] comments = issue.fields.comment.comments;
        final List<Comment> listComments = new ArrayList<>(comments.length);
        listComments.addAll(Arrays.asList(comments));
        return listComments.stream().filter(a -> a.isBefore(startDate)).collect(Collectors.toList());
    }

}

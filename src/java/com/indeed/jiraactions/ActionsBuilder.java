package com.indeed.jiraactions;

import com.indeed.jiraactions.api.response.issue.Issue;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.fields.comment.Comment;
import com.indeed.util.logging.Loggers;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ActionsBuilder {
    private static final Logger LOG = Logger.getLogger(ActionsBuilder.class);

    private final Issue issue;
    private final DateTime startDate;
    private final DateTime endDate;
    private final List<Action> actions;
    private final ActionFactory actionFactory;

    public ActionsBuilder(final ActionFactory actionFactory, final Issue issue, final DateTime startDate, final DateTime endDate) {
        this.actionFactory = actionFactory;
        this.issue = issue;
        this.startDate = startDate;
        this.endDate = endDate;

        actions = new ArrayList<>(issue.changelog.histories.length + issue.fields.comment.comments.length);
    }

    @Nonnull
    public List<Action> buildActions() throws IOException {
        setCreateAction();
        setUpdateActions();
        setCommentActions();
        return actions;
    }

    //
    // For Create Action
    //

    private void setCreateAction() throws IOException {
        final Action createAction = actionFactory.create(issue);
        actions.add(createAction);
    }

    //
    // For Update Action
    //

    private void setUpdateActions() {
        issue.changelog.sortHistories();

        Action prevAction = actions.get(actions.size()-1); // safe because we always add the create action
        for (final History history : issue.changelog.histories) {
            final Action updateAction = actionFactory.update(prevAction, history);
            actions.add(updateAction);
            prevAction = updateAction;
        }
    }

    //
    // For Comment Action
    //

    private void setCommentActions() {
        issue.fields.comment.sortComments();

        int currentActionIndex = 0;
        for (final Comment comment : issue.fields.comment.comments) {
            while (true) {
                if (commentIsRightAfter(comment, currentActionIndex)) {
                    final Action commentAction = actionFactory.comment(actions.get(currentActionIndex), comment);
                    actions.add(currentActionIndex + 1, commentAction);
                    break;
                } else {
                    currentActionIndex++;
                    if (currentActionIndex >= actions.size()) {
                    /* You'd think this would never happen, but it can. I found legitimate examples with a comment
                     * on a ticket *before* that ticket was created.
                     */
                        if (comment.created.isBefore(actions.get(0).getTimestamp())) {
                            Loggers.debug(LOG, "Skipping comment %s on %s because it's before the issue was created.",
                                    comment.id, issue.key);
                        } else {
                            Loggers.warn(LOG, "Unable to process comment %s by %s on issue %s, somehow doesn't fit in our timeline.",
                                    comment.id, comment.author.getDisplayName(), issue.key, comment.author.getDisplayName());
                        }
                        currentActionIndex = 0;
                        break;
                    }
                }
            }
        }
    }

    private boolean isCreatedDuringRange(final DateTime createdDate) {
        return startDate.compareTo(createdDate) <= 0 && endDate.compareTo(createdDate) > 0;
    }

    private boolean commentIsAfter(final Comment comment, final Action action) {
        /* return true if comment is made after the action. Or if it's the same instant as the action, because some
         * automated tools are that fast (or because of a comment made at the same time you do an edit.
         */
        final DateTime commentDate = comment.created;
        final DateTime actionDate = action.getTimestamp();
        return commentDate.isAfter(actionDate) || commentDate.isEqual(actionDate);
    }

    private boolean commentIsRightAfter(final Comment comment, final int actionIndex) {
        final Action action = actions.get(actionIndex);
        final int nextIndex = actionIndex + 1;
        final Action nextAction = actions.size() > nextIndex ? actions.get(nextIndex) : null;
        return commentIsAfter(comment, action) &&
                ( nextAction == null || !commentIsAfter(comment, nextAction) );
    }
}

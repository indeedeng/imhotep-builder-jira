package com.indeed.skeleton.index.builder.jiraaction;

import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.Comment;
import com.indeed.util.logging.Loggers;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * @author soono
 */
public class ActionsBuilder {
    private static final Logger log = Logger.getLogger(ActionsBuilder.class);

    private final boolean backfill;
    private final Issue issue;
    private final DateTime startDate;
    private final DateTime endDate;
    private boolean isNewIssue;
    public final List<Action> actions = new ArrayList<>();

    public ActionsBuilder(final Issue issue, final DateTime startDate, final DateTime endDate,
                          final boolean backfill) {
        this.issue = issue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.backfill = backfill;
    }

    public List<Action> buildActions() throws Exception {
        setIsNewIssue();

        if (isNewIssue) {
            setCreateAction();
        }

        setUpdateActions();

        setCommentActions();

        return actions;
    }

    private void setIsNewIssue() {
        this.isNewIssue = isCreatedDuringRange(issue.fields.created);
    }

    //
    // For Create Action
    //

    private void setCreateAction() throws Exception {
        final Action createAction = new Action(issue);
        actions.add(createAction);
    }

    //
    // For Update Action
    //

    private void setUpdateActions() throws Exception {
        issue.changelog.sortHistories();

        Action prevAction = getLatestAction();
        for (final History history : issue.changelog.histories) {
            if (!isCreatedDuringRange(history.created)) {
                continue;
            }
            final Action updateAction = new Action(prevAction, history);
            actions.add(updateAction);
            prevAction = updateAction;
        }
    }

    private Action getLatestAction() throws Exception {
        if (isNewIssue) {
            return actions.get(0); // returns create action.
        }
        else {
            // Get the last action by day before yesterday.
            Action action = new Action(issue);
            for (final History history : issue.changelog.histories) {
                if (isCreatedDuringRange(history.created)) {
                    break;
                }
                action = new Action(action, history);
            }
            return action;
        }
    }

    //
    // For Comment Action
    //

    private void setCommentActions() throws Exception {
        issue.fields.comment.sortComments();

        int currentActionIndex = 0;
        for (final Comment comment : issue.fields.comment.comments) {
            if (!comment.isValid()) {
                Loggers.warn(log, "Invalid comment for issue %s with id %s, created %s, updated %s, and body \"%s\".",
                        issue.key, comment.id, comment.created, comment.updated, comment.body);
            }
            if (!isCreatedDuringRange(comment.created)) {
                continue;
            }
            if (actions.isEmpty() || !commentIsAfter(comment, actions.get(0))) {
                final Action commentAction = new Action(getLatestAction(), comment);
                actions.add(0, commentAction);
            } else {
                while(true) {
                    if(commentIsRightAfter(comment, currentActionIndex)) {
                        final Action commentAction = new Action(actions.get(currentActionIndex), comment);
                        actions.add(currentActionIndex+1, commentAction);
                        break;
                    } else {
                        currentActionIndex++;
                    }
                }
            }
        }
    }


    //
    // For interpreting if the date is new
    //

    private boolean isCreatedDuringRange(final String dateString) {
        final DateTime createdDate = JiraActionUtil.parseDateTime(dateString);

        if(backfill) {
            return startDate.compareTo(createdDate) <= 0;
        }
        return startDate.compareTo(createdDate) <= 0 && endDate.compareTo(createdDate) == 1;
    }

    private boolean commentIsAfter(final Comment comment, final Action action) {
        // return true if comment is made after the action
        final DateTime commentDate = JiraActionUtil.parseDateTime(comment.created);
        final DateTime actionDate = JiraActionUtil.parseDateTime(action.timestamp);
        return commentDate.isAfter(actionDate);
    }

    private boolean commentIsRightAfter(final Comment comment, final int actionIndex) {
        final Action action = actions.get(actionIndex);
        final int nextIndex = actionIndex + 1;
        final Action nextAction = actions.size() > nextIndex ? actions.get(nextIndex) : null;
        return commentIsAfter(comment, action) &&
                ( nextAction == null || !commentIsAfter(comment, nextAction) );
    }
}

package com.indeed.skeleton.index.builder.jiraaction;

import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.Comment;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author soono
 */
public class ActionsBuilder {
    private static final Logger log = Logger.getLogger(ActionsBuilder.class);

    private final Issue issue;
    private final DateTime startDate;
    private final DateTime endDate;
    private final List<Action> actions;

    public ActionsBuilder(final Issue issue, final DateTime startDate, final DateTime endDate) {
        this.issue = issue;
        this.startDate = startDate;
        this.endDate = endDate;

        actions = new ArrayList<>(issue.changelog.histories.length + issue.fields.comment.comments.length);
    }

    public List<Action> buildActions() throws Exception {
        setCreateAction();
        setUpdateActions();
        setCommentActions();
        return actions.stream().filter(a -> isCreatedDuringRange(a.timestamp)).collect(Collectors.toList());
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

        Action prevAction = actions.get(actions.size()-1); // safe because we always add the create action
        for (final History history : issue.changelog.histories) {
            final Action updateAction = new Action(prevAction, history);
            actions.add(updateAction);
            prevAction = updateAction;
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
                log.warn(String.format("Invalid comment for issue %s with id %s, created %s, updated %s, and body \"%s\".",
                        issue.key, comment.id, comment.created, comment.updated, comment.body));
            }
            while (true) {
                if (commentIsRightAfter(comment, currentActionIndex)) {
                    final Action commentAction = new Action(actions.get(currentActionIndex), comment);
                    actions.add(currentActionIndex + 1, commentAction);
                    break;
                } else {
                    currentActionIndex++;
                }
            }
        }
    }


    //
    // For interpreting if the date is new
    //

    private boolean isCreatedDuringRange(final String dateString) {
        final DateTime createdDate = JiraActionUtil.parseDateTime(dateString);
        return startDate.compareTo(createdDate) <= 0 && endDate.compareTo(createdDate) == 1;
    }

    private boolean commentIsAfter(final Comment comment, final Action action) {
        /* return true if comment is made after the action. Or if it's the same instant as the action, because some
         * automated tools are that fast (or because of a comment made at the same time you do an edit.
         */
        final DateTime commentDate = JiraActionUtil.parseDateTime(comment.created);
        final DateTime actionDate = JiraActionUtil.parseDateTime(action.timestamp);
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

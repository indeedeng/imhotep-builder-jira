package com.indeed.skeleton.index.builder.jiraaction;

import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.Comment;
import com.indeed.userservice.UserServiceProtos;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by soono on 8/30/16.
 */
public class ActionsBuilder {
    private Issue issue;
    private boolean isNewIssue;
    public List<Action> actions = new ArrayList<Action>();

    public ActionsBuilder(Issue issue) throws ParseException {
        this.issue = issue;
        setIsNewIssue();
    }

    public List<Action> buildActions() throws Exception {

        if (isNewIssue) setCreateAction();

        setUpdateActions();

        setCommentActions();

        return actions;
    }

    private void setIsNewIssue() throws ParseException {
        this.isNewIssue = isOnYesterday(issue.fields.created);
    }

    //
    // For Create Action
    //

    private void setCreateAction() throws Exception {
        Action createAction = new Action(issue);
        actions.add(createAction);
    }

    //
    // For Update Action
    //

    private void setUpdateActions() throws Exception {
        issue.changelog.sortHistories();

        Action prevAction = getLatestAction();
        for (History history : issue.changelog.histories) {
            if (!isOnYesterday(history.created)) continue;
            Action updateAction = new Action(prevAction, history);
            actions.add(updateAction);
            prevAction = updateAction;
        }
    }

    private Action getLatestAction() throws Exception {
        if (isNewIssue) return actions.get(0); // returns create action.
        else {
            // Get the last action by day before yesterday.
            Action action = new Action(issue);
            for (History history : issue.changelog.histories) {
                if (isOnYesterday(history.created)) break;
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
        for (Comment comment : issue.fields.comment.comments) {
            if (!isOnYesterday(comment.created)) continue;
            if (actions.isEmpty() || !commentIsAfter(comment, actions.get(0))) {
                Action commentAction = new Action(getLatestAction(), comment);
                actions.add(0, commentAction);
            } else {
                while(true) {
                    if(commentIsRightAfter(comment, currentActionIndex)) {
                        Action commentAction = new Action(actions.get(currentActionIndex), comment);
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

    private boolean isOnYesterday(String dateString) throws ParseException {
        Calendar date = Calendar.getInstance();
        date.setTime(parseDate(dateString));

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        return date.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR)
                && date.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR);
    }

    private boolean commentIsAfter(Comment comment, Action action) throws ParseException {
        // return true if comment is made after the action
        Date commentDate = parseDate(comment.created);
        Date actionDate = parseDate(action.timestamp);
        return commentDate.after(actionDate);
    }

    private boolean commentIsRightAfter(Comment comment, int actionIndex) throws ParseException {
        Action action = actions.get(actionIndex);
        int nextIndex = actionIndex + 1;
        Action nextAction = actions.size() > nextIndex ? actions.get(nextIndex) : null;
        return commentIsAfter(comment, action) &&
                ( nextAction == null || !commentIsAfter(comment, nextAction) );
    }

    private Date parseDate(String dateString) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String strippedCreatedString = dateString.replace('T', ' ');
        Date date = dateFormat.parse(strippedCreatedString);
        return date;
    }
}

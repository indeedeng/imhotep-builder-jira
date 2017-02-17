package com.indeed.skeleton.index.builder.jiraaction;

import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.Comment;
import com.indeed.util.logging.Loggers;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

/**
 * @author soono on 8/31/16.
 */
public class Action {
    private static final Logger log = Logger.getLogger(Action.class);

    public String action;
    public final String actor;
    public String assignee;
    public String fieldschanged;
    public long issueage;
    public String issuekey;
    public String issuetype;
    public String project;
    public String prevstatus;
    public String reporter;
    public String resolution;
    public String status;
    public String summary;
    public long timeinstate;
    public long timesinceaction;
    public String timestamp;
    public String verifier;
    public String category;
    public String fixversions;

    // For Create Action
    public Action(final Issue issue) throws Exception {
        action = "create";
        actor = issue.fields.creator.displayName;
        assignee = issue.initialValue("assignee");
        fieldschanged = "created";
        issueage = 0;
        issuekey = issue.key;
        issuetype = issue.initialValue("issuetype");
        project = issue.initialValue("project");
        prevstatus = "";
        reporter = issue.initialValue("reporter");
        resolution = issue.initialValue("resolution");
        status = issue.initialValue("status");
        summary = issue.initialValue("summary");
        timeinstate = 0;
        timesinceaction = 0;
        timestamp = issue.fields.created;
        verifier = issue.initialValue("verifier");
        category = issue.initialValue("category");
        fixversions = issue.initialValue("fixversions");
    }

    // For Update Action
    public Action(final Action prevAction, final History history) {
        action = "update";
        actor = history.author.displayName;
        assignee = history.itemExist("assignee") ? history.getItemLastValue("assignee") : prevAction.assignee;
        fieldschanged = history.getChangedFields();
        issueage = prevAction.issueage + getTimeDiff(prevAction.timestamp, history.created);
        issuekey = prevAction.issuekey;
        issuetype = history.itemExist("issuetype") ? history.getItemLastValue("issuetype") : prevAction.issuetype;
        project = history.itemExist("project") ? history.getItemLastValue("project") : prevAction.project;
        prevstatus = prevAction.status;
        reporter = history.itemExist("reporter") ? history.getItemLastValue("reporter") : prevAction.reporter;
        resolution = history.itemExist("resolution") ? history.getItemLastValue("resolution") : prevAction.resolution;
        status = history.itemExist("status") ? history.getItemLastValue("status") : prevAction.status;
        summary = history.itemExist("summary") ? history.getItemLastValue("summary") : prevAction.summary;
        timeinstate = timeInState(prevAction, history);
        timesinceaction = getTimeDiff(prevAction.timestamp, history.created);
        timestamp = history.created;
        verifier = history.itemExist("verifier") ? history.getItemLastValue("verifier") : prevAction.verifier;
        category = history.itemExist("category") ? history.getItemLastValue("category") : prevAction.category;
        fixversions = history.itemExist("fixversions") ? history.getItemLastValue("fixversions") : prevAction.fixversions;
    }

    // For Comment Action
    public Action(final Action prevAction, final Comment comment) {
        action = "comment";
        actor =  comment.author.displayName;
        assignee = prevAction.assignee;
        fieldschanged = "comment";
        issueage = prevAction.issueage + getTimeDiff(prevAction.timestamp, comment.created);
        issuekey = prevAction.issuekey;
        issuetype = prevAction.issuetype;
        project = prevAction.project;
        prevstatus = prevAction.status;
        reporter = prevAction.reporter;
        resolution = prevAction.resolution;
        status = prevAction.status;
        summary = prevAction.summary;
        timeinstate = timeInState(prevAction, comment);
        timesinceaction = getTimeDiff(prevAction.timestamp, comment.created);
        timestamp = comment.created;
        verifier = prevAction.verifier;
        category = prevAction.category;
        fixversions = prevAction.fixversions;
    }

    private long timeInState(final Action prevAction, final Comment comment) {
        return timeInState(prevAction, comment.created);
    }

    private long timeInState(final Action prevAction, final History history) {
        return timeInState(prevAction, history.created);
    }

    private long timeInState(final Action prevAction, final String changeTimestamp) {
        if(!prevAction.prevstatus.equals(prevAction.status)) {
            return getTimeDiff(prevAction.timestamp, changeTimestamp);
        }

        return getTimeDiff(prevAction.timestamp, changeTimestamp) + prevAction.timeinstate;
    }

    private long getTimeDiff(final String before, final String after) {
        final DateTime beforeDate = JiraActionUtil.parseDateTime(before);
        final DateTime afterDate = JiraActionUtil.parseDateTime(after);
        final long seconds = (afterDate.getMillis() - beforeDate.getMillis()) / 1000;
        if(seconds < 0) {
            Loggers.error(log, "Invalid time difference between %s and %s for issue %s, action %s, fieldschanged %s.",
                    before, after, issuekey, action, fieldschanged);
        }
        return seconds;
    }

}

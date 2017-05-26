package com.indeed.jiraactions;

import com.indeed.jiraactions.api.response.issue.Issue;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.fields.comment.Comment;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

/**
 * @author soono on 8/31/16.
 */
public class Action {
    private static final Logger log = Logger.getLogger(Action.class);

    public String action;
    public String actor;
    public String actorusername;
    public String assignee;
    public String assigneeusername;
    public String fieldschanged;
    public long issueage;
    public String issuekey;
    public String issuetype;
    public String project;
    public String projectkey;
    public String prevstatus;
    public String reporter;
    public String reporterusername;
    public String resolution;
    public String status;
    public String summary;
    public long timeinstate;
    public long timesinceaction;
    public DateTime timestamp;
    public String verifier;
    public String verifierusername;
    public String category;
    public String fixversions;
    public String dueDate;
    public String components;
    public String labels;
    public String issueSizeEstimate;
    public String directCause;

    // For Create Action
    public Action(final Issue issue) throws Exception {
        action = "create";
        actor = issue.fields.creator == null ? "No User" : issue.fields.creator.displayName;
        actorusername = issue.fields.creator == null ? "No User" : issue.fields.creator.key;
        assignee = issue.initialValue("assignee");
        assigneeusername = issue.initialValue("assigneeusername");
        fieldschanged = "created";
        issueage = 0;
        issuekey = issue.key;
        issuetype = issue.initialValue("issuetype");
        project = issue.initialValue("project");
        projectkey = issue.initialValue("projectkey");
        prevstatus = "";
        reporter = issue.initialValue("reporter");
        reporterusername = issue.initialValue("reporterusername");
        resolution = issue.initialValue("resolution");
        status = issue.initialValue("status");
        summary = issue.initialValue("summary");
        timeinstate = 0;
        timesinceaction = 0;
        timestamp = issue.fields.created;
        verifier = issue.initialValue("verifier", true);
        verifierusername = issue.initialValue("verifierusername", true);
        category = issue.initialValue("category");
        fixversions = issue.initialValue("fixversions");
        dueDate = issue.initialValue("duedate");
        components = issue.initialValue("components");
        labels = issue.initialValue("labels");
        issueSizeEstimate = issue.initialValue("issuesizeestimate");
        directCause = issue.initialValue("directcause");
    }

    // For Update Action
    public Action(final Action prevAction, final History history) {
        action = "update";
        actor = history.author == null ? "No User" : history.author.displayName;
        actorusername = history.author == null ? "No User" : history.author.name;
        assignee = history.itemExist("assignee") ? history.getItemLastValue("assignee") : prevAction.assignee;
        assigneeusername = history.itemExist("assigneeusername") ? history.getItemLastValue("assigneeusername") : prevAction.assigneeusername;
        fieldschanged = history.getChangedFields();
        issueage = prevAction.issueage + getTimeDiff(prevAction.timestamp, history.created);
        issuekey = prevAction.issuekey;
        issuetype = history.itemExist("issuetype") ? history.getItemLastValue("issuetype") : prevAction.issuetype;
        project = history.itemExist("project") ? history.getItemLastValue("project") : prevAction.project;
        projectkey = history.itemExist("projectkey") ? history.getItemLastValue("projectkey") : prevAction.projectkey;
        prevstatus = prevAction.status;
        reporter = history.itemExist("reporter") ? history.getItemLastValue("reporter") : prevAction.reporter;
        reporterusername = history.itemExist("reporterusername") ? history.getItemLastValue("reporterusername") : prevAction.reporterusername;
        resolution = history.itemExist("resolution") ? history.getItemLastValue("resolution") : prevAction.resolution;
        status = history.itemExist("status") ? history.getItemLastValue("status") : prevAction.status;
        summary = history.itemExist("summary") ? history.getItemLastValue("summary") : prevAction.summary;
        timeinstate = timeInState(prevAction, history);
        timesinceaction = getTimeDiff(prevAction.timestamp, history.created);
        timestamp = history.created;
        verifier = history.itemExist("verifier", true) ? history.getItemLastValue("verifier", true) : prevAction.verifier;
        verifierusername = history.itemExist("verifierusername", true) ? history.getItemLastValue("verifierusername", true) : prevAction.verifierusername;
        category = history.itemExist("category") ? history.getItemLastValue("category") : prevAction.category;
        fixversions = history.itemExist("fixversions") ? history.getItemLastValue("fixversions") : prevAction.fixversions;
        dueDate = history.itemExist("duedate") ? history.getItemLastValue("duedate") : prevAction.dueDate;
        components = history.itemExist("components") ? history.getItemLastValue("copmonents") : prevAction.components;
        labels = history.itemExist("labels") ? history.getItemLastValue("labels") : prevAction.labels;
        issueSizeEstimate = history.itemExist("issuesizeestimate", true) ? history.getItemLastValue("issuesizeestimate", true) : prevAction.issueSizeEstimate;
        directCause = history.itemExist("directcause", true) ? history.getItemLastValue("directcause", true) : prevAction.directCause;
    }

    // For Comment Action
    public Action(final Action prevAction, final Comment comment) {
        action = "comment";
        actor =  comment.author == null ? "No User" : comment.author.displayName;
        actorusername =  comment.author == null ? "No User" : comment.author.name;
        assignee = prevAction.assignee;
        assigneeusername = prevAction.assigneeusername;
        fieldschanged = "comment";
        issueage = prevAction.issueage + getTimeDiff(prevAction.timestamp, comment.created);
        issuekey = prevAction.issuekey;
        issuetype = prevAction.issuetype;
        project = prevAction.project;
        projectkey = prevAction.projectkey;
        prevstatus = prevAction.status;
        reporter = prevAction.reporter;
        reporterusername = prevAction.reporterusername;
        resolution = prevAction.resolution;
        status = prevAction.status;
        summary = prevAction.summary;
        timeinstate = timeInState(prevAction, comment);
        timesinceaction = getTimeDiff(prevAction.timestamp, comment.created);
        timestamp = comment.created;
        verifier = prevAction.verifier;
        verifierusername = prevAction.verifierusername;
        category = prevAction.category;
        fixversions = prevAction.fixversions;
        dueDate = prevAction.dueDate;
        components = prevAction.components;
        labels = prevAction.labels;
        issueSizeEstimate = prevAction.issueSizeEstimate;
        directCause = prevAction.directCause;
    }

    private long timeInState(final Action prevAction, final Comment comment) {
        return timeInState(prevAction, comment.created);
    }

    private long timeInState(final Action prevAction, final History history) {
        return timeInState(prevAction, history.created);
    }

    private long timeInState(final Action prevAction, final DateTime changeTimestamp) {
        if(!prevAction.prevstatus.equals(prevAction.status)) {
            return getTimeDiff(prevAction.timestamp, changeTimestamp);
        }

        return getTimeDiff(prevAction.timestamp, changeTimestamp) + prevAction.timeinstate;
    }

    private long getTimeDiff(final DateTime before, final DateTime after) {
        final long seconds = (after.getMillis() - before.getMillis()) / 1000;
        if(seconds < 0) {
            log.error(String.format("Invalid time difference between %s and %s for issue %s, action %s, fieldschanged %s.",
                    before, after, issuekey, action, fieldschanged));
        }
        return seconds;
    }

}

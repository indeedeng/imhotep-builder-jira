package com.indeed.skeleton.index.builder.jiraaction;

import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.Comment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by soono on 8/31/16.
 */
public class Action {
    public String action;
    public String actor;
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
    public String timestamp;
    public String verifier;

    // For Create Action
    public Action(Issue issue) throws Exception {
        action = "create";
        actor = issue.fields.creator.displayName;
        assignee = issue.initialValue("assignee");
        fieldschanged = "";
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
        timestamp = issue.fields.created;
        verifier = issue.initialValue("verifier");
    }

    // For Update Action
    public Action(Action prevAction, History history) throws ParseException {
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
        timeinstate = getTimeDiff(prevAction.timestamp, history.created);;
        timestamp = history.created;
        verifier = history.itemExist("verifier") ? history.getItemLastValue("verifier") : prevAction.verifier;
    }

    // For Comment Action
    public Action(Action prevAction, Comment comment) throws ParseException {
        action = "comment";
        actor =  comment.author.displayName;
        assignee = prevAction.assignee;
        fieldschanged = "";
        issueage = prevAction.issueage + getTimeDiff(prevAction.timestamp, comment.created);
        issuekey = prevAction.issuekey;
        issuetype = prevAction.issuetype;
        project = prevAction.project;
        prevstatus = prevAction.status;
        reporter = prevAction.reporter;
        resolution = prevAction.resolution;
        status = prevAction.status;
        summary = prevAction.summary;
        timeinstate = timeinstateForComment(prevAction, comment);
        timestamp = comment.created;
        verifier = prevAction.verifier;
    }

    private long timeinstateForComment(Action prevAction, Comment comment) throws ParseException {
        if (prevAction.action.equals("comment")) {
            return prevAction.timeinstate + getTimeDiff(prevAction.timestamp, comment.created);
        } else {
            return getTimeDiff(prevAction.timestamp, comment.created);
        }
    }

    private long getTimeDiff(String before, String after) throws ParseException {
        Date beforeDate = parseDate(before);
        Date afterDate = parseDate(after);
        long seconds = (afterDate.getTime() - beforeDate.getTime()) / 1000;
        return seconds;
    }

    private Date parseDate(String dateString) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String strippedCreatedString = dateString.replace('T', ' ');
        Date date = dateFormat.parse(strippedCreatedString);
        return date;
    }

}

package com.indeed.jiraactions.api.response.issue.fields;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.indeed.jiraactions.JiraActionsUtil;
import com.indeed.jiraactions.api.response.issue.Priority;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.jiraactions.api.response.issue.fields.comment.CommentCollection;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("CanBeFinal")
public class Field {
    public User assignee;
    public CommentCollection comment;
    public DateTime created;
    public User creator;
    public Issuetype issuetype;
    public Project project;
    public User reporter;
    public Resolution resolution;
    public Status status;
    public String summary;
    public FixVersion[] fixVersions;
    public String duedate;
    public Component[] components;
    public String[] labels;
    public Priority priority;
    public Map<String, JsonNode> otherProperties = new HashMap<>();

    @JsonProperty("created")
    public void setCreate(final String created) {
        this.created = JiraActionsUtil.parseDateTime(created);
    }

    @SuppressWarnings("unused")
    @JsonAnySetter
    public void setOtherProperty(final String key, final JsonNode value) {
        otherProperties.put(key, value);
    }

    @Nullable
    public JsonNode getCustomField(final String attribute) {
        return otherProperties.get(attribute);
    }

    public String getStringValue(final String attribute) throws IOException {
        switch (attribute) {
            case "assignee": return assignee == null ? "" : assignee.displayName;
            case "assigneekey": return assignee == null ? "" : assignee.key;
            case "creator": return creator == null ? "" : creator.displayName;
            case "issuetype": return issuetype == null ? "" : issuetype.name;
            case "project": return project == null ? "" : project.name;
            case "projectkey": return project == null ? "" : project.key;
            case "reporter": return reporter == null ? "" : reporter.displayName;
            case "reporterusername": return reporter == null ? "" : reporter.name;
            case "resolution": return resolution == null? "" : resolution.name;
            case "status": return status == null? "" : status.name;
            case "summary": return summary;
            case "category": {
                final ProjectCategory category = project == null
                        ? null
                        : project.projectCategory;
                return (category == null || category.name == null)
                        ? ""
                        : category.name;
            }
            case "fixversions": return fixVersions == null ? "" : Joiner.on("|").join(fixVersions);
            case "duedate": return duedate == null ? "" : duedate;
            case "components": return components == null ? "" : Joiner.on("|").join(components);
            case "labels": return labels == null ? "" : Joiner.on(" ").join(labels);
            case "priority": return priority == null ? "" : priority.name;
        }
        throw new IOException("Wrong input name " + attribute);
    }
}

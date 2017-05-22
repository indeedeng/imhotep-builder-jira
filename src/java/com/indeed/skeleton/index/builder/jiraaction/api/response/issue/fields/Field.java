package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import com.indeed.skeleton.index.builder.jiraaction.JiraActionUtil;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.User;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.CommentCollection;
import org.joda.time.DateTime;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)
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
    public User verifier;
    public FixVersion[] fixVersions;
    public String duedate;
    public Component[] components;
    public String[] labels;
    public IssueSizeEstimate issuesizeestimate;

    @SuppressWarnings("unused")
    @JsonProperty("created")
    public void setCreate(final String created) {
        this.created = JiraActionUtil.parseDateTime(created);
    }

    @SuppressWarnings("unused")
    @JsonProperty("customfield_10003")
    public void setVerifier(final User verifier) {
        this.verifier = verifier;
    }

    public String getStringValue(final String attribute) throws Exception {
        switch (attribute) {
            case "assignee": return assignee == null ? "" : assignee.displayName;
            case "creator": return creator == null ? "" : creator.displayName;
            case "issuetype": return issuetype == null ? "" : issuetype.name;
            case "project": return project == null ? "" : project.name;
            case "reporter": return reporter == null ? "" : reporter.displayName;
            case "resolution": return resolution == null? "" : resolution.name;
            case "status": return status == null? "" : status.name;
            case "summary": return summary;
            case "verifier": return verifier == null? "" : verifier.displayName;
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
            case "issuesizeestimate": return issuesizeestimate == null ? "" : issuesizeestimate.value;
        }
        throw new Exception("Wrong Input name");
    }
}

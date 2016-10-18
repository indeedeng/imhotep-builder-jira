package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment.CommentCollection;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.User;

/**
 * Created by soono on 8/25/16.
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class Field {
    public final User assignee;
    public CommentCollection comment;
    public String created;
    public User creator;
    public final Issuetype issuetype;
    public final Project project;
    public final User reporter;
    public final Resolution resolution;
    public final Status status;
    public final String summary;
    public User verifier;

    @JsonProperty("customfield_10003")
    public void setVerifier(final User verifier) {
        this.verifier = verifier;
    }

    public String getStringValue(final String attribute) throws Exception {
        switch (attribute) {
            case "assignee": return assignee == null ? "" : assignee.displayName;
            case "created": return created;
            case "creator": return creator == null ? "" : creator.displayName;
            case "issuetype": return issuetype == null ? "" : issuetype.name;
            case "project": return project == null ? "" : project.name;
            case "reporter": return reporter == null ? "" : reporter.displayName;
            case "resolution": return resolution == null? "" : resolution.name;
            case "status": return status == null? "" : status.name;
            case "summary": return summary;
            case "verifier": return verifier == null? "" : verifier.displayName;
        }
        throw new Exception("Wrong Input name");
    }
}

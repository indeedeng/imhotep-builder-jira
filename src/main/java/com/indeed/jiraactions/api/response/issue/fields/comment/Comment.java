package com.indeed.jiraactions.api.response.issue.fields.comment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.indeed.jiraactions.JiraActionsUtil;
import com.indeed.jiraactions.api.response.issue.User;
import org.joda.time.DateTime;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class Comment {
    public String id;
    public User author;
    public String body;
    public DateTime created;

    public boolean isValid() {
        return author != null;
    }

    @JsonProperty("created")
    public void setCreate(final String created) {
        this.created = JiraActionsUtil.parseDateTime(created);
    }
}

package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by soono on 8/25/16.
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class CommentCollection {
    public Comment[] comments;
}

package com.indeed.skeleton.index.builder.jiraaction.api.response.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by soono on 8/25/16.
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class User {
    public String displayName;
}

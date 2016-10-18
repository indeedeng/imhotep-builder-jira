package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by soono on 9/9/16.
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class Project {
    public String name;
}

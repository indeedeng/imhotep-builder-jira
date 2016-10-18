package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by soono on 8/31/16.
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class Status {
    public final String name;
}

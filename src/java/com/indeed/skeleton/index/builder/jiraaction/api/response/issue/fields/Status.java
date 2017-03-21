package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class Status {
    public String name;
}

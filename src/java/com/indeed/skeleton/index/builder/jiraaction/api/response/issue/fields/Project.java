package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class Project {
    public ProjectCategory projectCategory;
    public String name;
}

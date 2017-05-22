package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author kbinswanger
 */
@JsonIgnoreProperties(ignoreUnknown=true)

public class Component {
    public String name;

    @Override
    public String toString() {
        return name;
    }
}
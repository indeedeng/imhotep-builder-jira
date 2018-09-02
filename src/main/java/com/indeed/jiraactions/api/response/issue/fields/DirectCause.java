package com.indeed.jiraactions.api.response.issue.fields;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author kbinswanger
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class DirectCause {
    public String value;
    public DirectCause child;

    @Override
    public String toString() {
        return value + (child == null ? "" : " - " + child.toString());
    }
}
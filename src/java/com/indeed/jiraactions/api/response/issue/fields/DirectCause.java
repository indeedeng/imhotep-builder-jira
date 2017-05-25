package com.indeed.jiraactions.api.response.issue.fields;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author kbinswanger
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class DirectCause {
    public String value;

    @Override
    public String toString() {
        return value;
    }
}
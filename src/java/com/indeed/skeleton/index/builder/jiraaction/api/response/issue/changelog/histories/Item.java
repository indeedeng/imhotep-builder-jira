package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class Item {
    public String field;
    public String fromString;
    public String toString;

    @JsonProperty("field")
    public void setVerifier(final String field) {
        this.field = field.toLowerCase().replaceAll("\\s", "-");
    }
}

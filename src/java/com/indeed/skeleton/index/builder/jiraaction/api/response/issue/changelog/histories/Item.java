package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by soono on 8/25/16.
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class Item {
    public String field;
    public String fieldtype;
    public String from;
    public String fromString;
    public String to;
    public String toString;

    @JsonProperty("field")
    public void setVerifier(String field) {
        this.field = field.toLowerCase().replaceAll("\\s", "-");
    }
}

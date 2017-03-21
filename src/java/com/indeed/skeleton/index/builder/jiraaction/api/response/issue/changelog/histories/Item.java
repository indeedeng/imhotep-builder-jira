package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import org.python.google.common.annotations.VisibleForTesting;

import java.util.Map;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class Item {
    @VisibleForTesting
    protected static final Map<String, String> jiraFieldMapping = ImmutableMap.<String, String>builder()
            .put("Fix Version", "fixversions")
            .build();

    public String field;
    public String fromString;
    public String toString;

    @JsonProperty("field")
    public void setField(final String field) {
        if(jiraFieldMapping.containsKey(field)) {
            this.field = jiraFieldMapping.get(field);
        } else {
            this.field = field.toLowerCase().replaceAll("\\s", "-");
        }
    }
}

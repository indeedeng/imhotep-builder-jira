package com.indeed.jiraactions.api.response.issue.changelog.histories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class Item {
    private static final Logger log = Logger.getLogger(Item.class);

    public String field;
    public String fromString;
    public String toString;
    public boolean customField;

    @VisibleForTesting
    protected static final Map<String, String> jiraFieldMapping = ImmutableMap.<String, String>builder()
            .put("Fix Version", "fixversions")
            .build();

    @JsonProperty("fieldtype")
    public void setFieldType(final String fieldType) {
        if("custom".equals(fieldType)) {
            customField = true;
        } else if(!"jira".equals(fieldType)) {
            log.warn(String.format("Invalid fieldtype %s for field '%s' from '%s' to '%s'", fieldType,
                    field, fromString, toString));
        }
    }

    @JsonProperty("field")
    public void setField(final String field) {
        if(jiraFieldMapping.containsKey(field)) {
            this.field = jiraFieldMapping.get(field);
        } else {
            this.field = field.toLowerCase().replaceAll("\\s", "-");
        }
    }
}

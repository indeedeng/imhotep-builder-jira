package com.indeed.jiraactions.api.response.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.indeed.jiraactions.api.response.issue.changelog.ChangeLog;
import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;
import com.indeed.jiraactions.api.response.issue.fields.Field;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)
@SuppressWarnings("CanBeFinal")
public class Issue {
    public String key;
    public Field fields;
    public ChangeLog changelog;

    public String initialValue(final String field) throws IOException {
        return initialValue(field, field);
    }

    public String initialValue(final String field, final String fallbackField) throws IOException {
        final Item history = initialItem(false, field);
        if(history != null) {
            return history.fromString == null ? "" : history.fromString;
        } else {
            return this.fields.getStringValue(fallbackField);
        }
    }

    // This name sucks
    public String initialValueKey(final String field, final String fallbackField) throws IOException {
        final Item history = initialItem(false, field);
        if(history != null) {
            return history.from == null ? "" : history.from;
        } else {
            return this.fields.getStringValue(fallbackField);
        }
    }

    @Nullable
    public Item initialItem(final boolean acceptCustom, final String... fields) {
        return this.changelog.getFirstHistoryItem(acceptCustom, fields);
    }

    public String toString() {
        return key;
    }
}

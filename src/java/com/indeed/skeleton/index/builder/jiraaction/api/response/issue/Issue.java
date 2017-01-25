package com.indeed.skeleton.index.builder.jiraaction.api.response.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.ChangeLog;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.Item;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.Field;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class Issue {
    public String key;
    public Field fields;
    public ChangeLog changelog;

    public String initialValue(final String field) throws Exception {
        if (this.changelog.historyItemExist(field)) {
            final Item history = this.changelog.getFirstHistoryItem(field);
            final String fromString = history != null ? history.fromString : null;
            return fromString == null ? "" : fromString;
        } else {
            return this.fields.getStringValue(field);
        }
    }
}

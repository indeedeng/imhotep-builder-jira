package com.indeed.skeleton.index.builder.jiraaction.api.response.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.ChangeLog;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.Field;

/**
 * Created by soono on 8/24/16.
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class Issue {
    public final String key;
    public Field fields;
    public ChangeLog changelog;

    public String initialValue(final String field) throws Exception {
        if (this.changelog.historyItemExist(field)) {
            final String fromString = this.changelog.getFirstHistoryItem(field).fromString;
            return fromString == null ? "" : fromString;
        } else {
            return this.fields.getStringValue(field);
        }
    }
}

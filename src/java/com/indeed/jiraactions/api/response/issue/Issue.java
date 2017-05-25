package com.indeed.jiraactions.api.response.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.indeed.jiraactions.api.response.issue.changelog.ChangeLog;
import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;
import com.indeed.jiraactions.api.response.issue.fields.Field;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)
@SuppressWarnings("CanBeFinal")
public class Issue {
    public String key;
    public Field fields;
    public ChangeLog changelog;

    public String initialValue(final String field) throws Exception {
        return initialValue(field, false);
    }

    public String initialValue(final String field, final boolean acceptCustom) throws Exception {
        if (this.changelog.historyItemExist(field, acceptCustom)) {
            final Item history = this.changelog.getFirstHistoryItem(field, acceptCustom);
            final String fromString = history != null ? history.fromString : null;
            return fromString == null ? "" : fromString;
        } else {
            return this.fields.getStringValue(field);
        }
    }
}

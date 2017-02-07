package com.indeed.skeleton.index.builder.jiraaction.api.response.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.indeed.skeleton.index.builder.jiraaction.ActionsBuilder;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.ChangeLog;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.Item;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.Field;
import com.indeed.util.logging.Loggers;
import org.apache.log4j.Logger;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class Issue {
    private static final Logger log = Logger.getLogger(ActionsBuilder.class);

    public String key;
    public Field fields;
    public ChangeLog changelog;

    public String initialValue(final String field) throws Exception {
        if (this.changelog.historyItemExist(field)) {
            final Item history = this.changelog.getFirstHistoryItem(field);
            final String fromString = history != null ? history.fromString : null;
            return fromString == null ? "" : fromString;
        } else {
            Loggers.warn(log, "Using initial value of %s for %s, potential error?",
                    fields.getStringValue(field), field);
            return this.fields.getStringValue(field);
        }
    }
}

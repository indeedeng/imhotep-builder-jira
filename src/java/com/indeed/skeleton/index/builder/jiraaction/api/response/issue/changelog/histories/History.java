package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.User;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class History {
    public User author;
    public String created;
    public Item[] items;

    public String getChangedFields() {
        final StringBuilder fieldsChanged = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i != 0) fieldsChanged.append(" ");
            fieldsChanged.append(items[i].field);
        }
        return fieldsChanged.toString();
    }

    public boolean itemExist(final String field) {
        for (final Item item : items) {
            if (item.field.equals(field)) {
                return true;
            }
        }
        return false;
    }

    public String getItemLastValue(final String field) {
        for (final Item item : items) {
            if (item.field.equals(field)) {
                final String toString = item.toString;
                return toString == null ? "" : toString;
            }
        }
        return "";
    }
}

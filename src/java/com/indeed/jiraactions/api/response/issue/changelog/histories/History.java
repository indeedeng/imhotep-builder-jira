package com.indeed.jiraactions.api.response.issue.changelog.histories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.indeed.jiraactions.JiraActionsUtil;
import com.indeed.jiraactions.api.response.issue.User;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class History {
    public User author;
    public DateTime created;
    public Item[] items;

    @SuppressWarnings("unused")
    @JsonProperty("created")
    public void setCreate(final String created) {
        this.created = JiraActionsUtil.parseDateTime(created);
    }

    public String getChangedFields() {
        final StringBuilder fieldsChanged = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i != 0) fieldsChanged.append(" ");
            fieldsChanged.append(items[i].field);
        }
        return fieldsChanged.toString();
    }

    public boolean itemExist(final String field) {
        return itemExist(field, false);
    }

    public boolean itemExist(final String field, final boolean acceptCustom) {
        for (final Item item : items) {
            if (item.field.equals(field) && (acceptCustom || !item.customField)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Item getItem(final String field, final boolean acceptCustom) {
        for (final Item item : items) {
            if (item.field.equals(field) && (acceptCustom || !item.customField)) {
                return item;
            }
        }

        return null;
    }

    public String getItemLastValue(final String field) {
        return getItemLastValue(field, false);
    }

    public String getItemLastValue(final String field, final boolean acceptCustom) {
        final Item item = getItem(field, acceptCustom);
        if (item == null || item.toString == null) {
            return "";
        }

        return item.toString;
    }
}

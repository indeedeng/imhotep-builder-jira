package com.indeed.jiraactions.api.response.issue.changelog.histories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import com.indeed.jiraactions.JiraActionsUtil;
import com.indeed.jiraactions.api.response.issue.User;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
        final Set<String> fieldsChanged = new HashSet<>();
        for (final Item item : items) {
            fieldsChanged.add(item.field);
        }
        return Joiner.on(" ").join(fieldsChanged.iterator());
    }

    public boolean itemExist(final String field) {
        return itemExist(field, false);
    }

    public boolean itemExist(final String field, final boolean acceptCustom) {
        for (final Item item : items) {
            if (Objects.equals(item.field, field) && (acceptCustom || !item.customField)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Item getItem(final String field, final boolean acceptCustom) {
        for (final Item item : items) {
            if (Objects.equals(item.field, field) && (acceptCustom || !item.customField)) {
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

    @SuppressWarnings("SameParameterValue")
    public String getItemLastValueParent(final String field, final boolean acceptCustom) {
        final Item item = getItem(field, acceptCustom);
        if(item == null || item.toString == null) {
            return "";
        }

        final int start = item.toString.indexOf("Parent values: ") + "Parent values: ".length();
        if(start < 0) {
            return item.toString;
        }

        final int end = item.toString.indexOf("(");

        return item.toString.substring(start, end);
    }

    @SuppressWarnings("SameParameterValue")
    public String getItemLastValueChild(final String field, final boolean acceptCustom) {
        final Item item = getItem(field, acceptCustom);
        if(item == null || item.toString == null) {
            return "";
        }

        final int start = item.toString.indexOf("Level 1 values: ") + "Level 1 values: ".length();
        if(start < 0) {
            return item.toString;
        }

        final int end = item.toString.lastIndexOf("(");

        return item.toString.substring(start, end);
    }

    // Of the form "Parent values: Escaped bug(20664)Level 1 values: Latent Code Issue(20681)"
    @SuppressWarnings("SameParameterValue")
    public String getItemLastValueFlattened(final String field, final boolean acceptCustom) {
        final Item item = getItem(field, acceptCustom);
        if(item == null || item.toString == null) {
            return "";
        }

        final int start = item.toString.indexOf("Parent values: ") + "Parent values: ".length();
        if(start < 0) {
            return item.toString;
        }

        return item.toString.substring(start)
                .replaceAll("\\(\\d+\\)", "")
                .replaceAll("Level \\d values: ", " - ");
    }

    public String getItemLastValueKey(final String field) {
        return getItemLastValueKey(field, false);
    }

    public String getItemLastValueKey(final String field, final boolean acceptCustom) {
        final Item item = getItem(field, acceptCustom);
        if (item == null || item.to == null) {
            return "";
        }

        return item.to;
    }
}

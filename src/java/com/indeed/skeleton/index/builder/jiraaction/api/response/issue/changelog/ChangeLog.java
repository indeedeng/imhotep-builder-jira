package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.indeed.skeleton.index.builder.jiraaction.JiraActionUtil;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.Item;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class ChangeLog {
    public History[] histories;

    public boolean historyItemExist(final String field) {
        for (final History history : histories) {
            if (history.itemExist(field)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Item getFirstHistoryItem(final String field) {
        // Return the first history item about the field.
        // If there is no history item about the field, return null.

        // Sort history items in time order.
        for (final History history : histories) {
            for (final Item item : history.items) {
                if (item.field.equals(field)) {
                    return item;
                }
            }
        }
        return null;
    }

    public void sortHistories() {
        // It seems JIRA API's response is already sorted, but
        // just in case, use this method to make sure.
        // Because it's usually already sorted, use insertion sort algorithm here.

        for (int i=1; i < histories.length; i++) {
            final History history = histories[i];
            final DateTime date = JiraActionUtil.parseDateTime(history.created);
            int j;
            for (j=i-1; j >= 0; j--) {
                final DateTime comparedDate = JiraActionUtil.parseDateTime(histories[j].created);
                if (date.isAfter(comparedDate) || date.equals(comparedDate)) {
                    break;
                }
                histories[j+1] = histories[j];
            }
            histories[j+1] = history;
        }
    }
}

package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.Item;

/**
 * Created by soono on 8/25/16.
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class ChangeLog {
    public History[] histories;

    public boolean historyItemExist(String field) {
        for (History history : histories) {
            if (history.itemExist(field)) return true;
        }
        return false;
    }

    public Item getFirstHistoryItem(String field) {
        // Return the first history item about the field.
        // If there is no history item about the field, return null.

        // Sort history items in time order.
        for (History history : histories) {
            for (Item item : history.items) {
                if (item.field.equals(field)) return item;
            }
        }
        return null;
    }
}

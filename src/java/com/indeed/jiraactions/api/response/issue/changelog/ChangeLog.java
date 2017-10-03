package com.indeed.jiraactions.api.response.issue.changelog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class ChangeLog {
    public History[] histories;

    @Nullable
    public Item getFirstHistoryItem(final boolean acceptCustom, final String... fields) {
        for (final History history : histories) {
            for(final String field : fields) {
                final Item item = history.getItem(field, acceptCustom);
                if(item != null) {
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
            final DateTime date = history.created;
            int j;
            for (j=i-1; j >= 0; j--) {
                final DateTime comparedDate = histories[j].created;
                if (date.isAfter(comparedDate) || date.equals(comparedDate)) {
                    break;
                }
                histories[j+1] = histories[j];
            }
            histories[j+1] = history;
        }
    }
}

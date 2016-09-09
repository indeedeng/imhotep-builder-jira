package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.History;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories.Item;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    public void sortHistories() throws ParseException {
        // It seems JIRA API's response is already sorted, but
        // just in case, use this method to make sure.
        // Because it's usually already sorted, use insertion sort algorithm here.

        for (int i=1; i<histories.length; i++) {
            History history = histories[i];
            Date date = parseDate(history.created);
            for (int k=i-1; k>=0; k--) {
                History comparedHistory = histories[k];
                Date comparedDate = parseDate(comparedHistory.created);
                if (date.after(comparedDate)) {
                    histories[k+1] = history;
                    break;
                }
                else {
                    histories[k+1] = histories[k];
                }
            }
        }
    }

    private Date parseDate(String dateString) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String strippedCreatedString = dateString.replace('T', ' ');
        Date date = dateFormat.parse(strippedCreatedString);
        return date;
    }
}

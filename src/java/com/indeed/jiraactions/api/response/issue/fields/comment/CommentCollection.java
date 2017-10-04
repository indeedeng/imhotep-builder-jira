package com.indeed.jiraactions.api.response.issue.fields.comment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;

import java.util.Arrays;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class CommentCollection {
    public Comment[] comments;

    public void sortComments() {
        // It seems JIRA API's response is already sorted, but
        // just in case, use this method to make sure.
        // Because it's usually already sorted, use insertion sort algorithm here.
        // KB: This comes back in *updated* order instead of created order

        Arrays.sort(comments, (o1, o2) -> {
            final DateTime date1 = o1.created;
            final DateTime date2 = o2.created;
            return date1.compareTo(date2);
        });
    }
}

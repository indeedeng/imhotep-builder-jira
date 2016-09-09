package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.fields.comment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by soono on 8/25/16.
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class CommentCollection {
    public Comment[] comments;

    public void sortComments() throws ParseException {
        // It seems JIRA API's response is already sorted, but
        // just in case, use this method to make sure.
        // Because it's usually already sorted, use insertion sort algorithm here.

        for (int i=1; i<comments.length; i++) {
            Comment comment = comments[i];
            Date date = parseDate(comment.created);
            for (int k=i-1; k>=0; k--) {
                Comment comparedComment = comments[k];
                Date comparedDate = parseDate(comparedComment.created);
                if (date.after(comparedDate)) {
                    comments[k+1] = comment;
                    break;
                }
                else {
                    comments[k+1] = comments[k];
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

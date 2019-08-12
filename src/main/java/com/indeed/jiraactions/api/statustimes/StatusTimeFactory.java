package com.indeed.jiraactions.api.statustimes;

import com.indeed.jiraactions.Action;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.fields.comment.Comment;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

public class StatusTimeFactory {

    public static Map<String, StatusTime> firstStatusTime(@Nonnull final String status) {  // Used for the first ever status
        final Map<String, StatusTime> statusTimeMap = new HashMap<>();
        final StatusTime st = ImmutableStatusTime.builder()
                .timetofirst(0)
                .timetolast(0)
                .timeinstatus(0)
                .build();
        statusTimeMap.put(status, st);
        return statusTimeMap;
    }

    private static StatusTime addStatus(@Nonnull final long timetofirst, final long timetolast) {
        return ImmutableStatusTime.builder()
                .timeinstatus(0)
                .timetofirst(timetofirst)
                .timetolast(timetolast)
                .build();
    }

    private static StatusTime updateTime(@Nonnull final StatusTime prev, final long time) {
        return ImmutableStatusTime.builder()
                .from(prev)
                .timeinstatus(prev.getTimeinstatus() + time)
                .build();
    }

    private static StatusTime updateTimeToLast(@Nonnull final StatusTime prev, final long timetolast) {
        return ImmutableStatusTime.builder()
                .from(prev)
                .timetolast(timetolast)
                .build();
    }

    public static Map<String, StatusTime> getStatusTimeUpdate(final Map<String, StatusTime> prevMap, final History history, final Action prevAction) {
        final Map<String, StatusTime> statusTimeMap = new HashMap<>(prevMap);
        final String status = history.itemExist("status") ? history.getItemLastValue("status") : prevAction.getStatus();
        statusTimeMap.replace(prevAction.getStatus(), updateTime(statusTimeMap.get(prevAction.getStatus()), getTimeDiff(prevAction.getTimestamp(), history.created)));
        if (statusTimeMap.containsKey(status)) {
            statusTimeMap.replace(status, updateTime(statusTimeMap.get(status), getTimeDiff(prevAction.getTimestamp(), history.created)));
            if (!prevAction.getStatus().equals(status)) {
                statusTimeMap.replace(status, updateTimeToLast(statusTimeMap.get(status), prevAction.getIssueage() + getTimeDiff(prevAction.getTimestamp(), history.created)));
            }
        } else {
            statusTimeMap.put(status, addStatus(prevAction.getIssueage() + getTimeDiff(prevAction.getTimestamp(), history.created), prevAction.getIssueage() + getTimeDiff(prevAction.getTimestamp(), history.created)));
        }
        return statusTimeMap;
    }

    public static Map<String, StatusTime> getStatusTimeComment(final Map<String, StatusTime> prevMap, final Comment comment, final Action prevAction) {
        final Map<String, StatusTime> statusTimeMap = new HashMap<>(prevMap);
        final String status = prevAction.getStatus();
        statusTimeMap.replace(status, updateTime(statusTimeMap.get(status), getTimeDiff(prevAction.getTimestamp(), comment.created)));
        return statusTimeMap;
    }

    public static Map<String, StatusTime> getStatusTimeCurrent(final Map<String, StatusTime> prevMap, final Action prevAction, final DateTime endDate) {
        final Map<String, StatusTime> statusTimeMap = new HashMap<>(prevMap);
        final String status = prevAction.getStatus();
        statusTimeMap.replace(status, updateTime(statusTimeMap.get(status), getTimeDiff(prevAction.getTimestamp(), endDate)));
        return statusTimeMap;
    }

    private static long getTimeDiff(final DateTime before, final DateTime after) {
        return (after.getMillis() - before.getMillis()) / 1000;
    }
}

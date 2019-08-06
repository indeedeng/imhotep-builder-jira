package com.indeed.jiraactions.api.statustimes;

import com.indeed.jiraactions.Action;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.fields.comment.Comment;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

public class StatusTimeFactory {

    public List<StatusTime> firstStatusTime(@Nonnull final String status) {
        List<StatusTime> st = new ArrayList<>();
        st.add(addStatus(status, 0, 0));
        return st;
    }

    public StatusTime addStatus(@Nonnull final String status, final long timetofirst, final long timetolast) {
        return ImmutableStatusTime.builder()
                .status(status)
                .timeinstatus(0)
                .timetofirst(timetofirst)
                .timetolast(timetolast)
                .build();
    }

    public StatusTime updateStatus(@Nonnull final StatusTime prev, final long time) {
        return ImmutableStatusTime.builder()
                .from(prev)
                .status(prev.getStatus())
                .timeinstatus(prev.getTimeinstatus() + time)
                .build();
    }

    public StatusTime updateTimeToLast(@Nonnull final StatusTime prev) {
        return ImmutableStatusTime.builder()
                .from(prev)
                .timetolast(0)
                .build();
    }

    public List<StatusTime> getStatusTimeUpdate(final List<StatusTime> list, final History history, final Action prevAction) {
        final List<StatusTime> st = new ArrayList<>(list);
        String status = history.itemExist("status") ? history.getItemLastValue("status") : prevAction.getStatus();
        st.set(st.size()-1, updateStatus(st.get(st.size()-1), getTimeDiff(prevAction.getTimestamp(), history.created)));
        if (!status.equals(prevAction.getStatus())) {
            boolean first = true;
            for(int i = 0; i < st.size(); i++) {
                if (st.get(i).getStatus().equals(status)) {
                    st.set(i, updateTimeToLast(st.get(i)));
                    first = false;
                }
            }
            if (first) {
                st.add(addStatus(status, prevAction.getIssueage() + getTimeDiff(prevAction.getTimestamp(), history.created), prevAction.getIssueage() + getTimeDiff(prevAction.getTimestamp(), history.created)));
            } else {
                st.add(addStatus(status, 0, prevAction.getIssueage() + getTimeDiff(prevAction.getTimestamp(), history.created)));
            }

        }
        return st;
    }

    public List<StatusTime> getStatusTimeComment(final List<StatusTime> list, final Comment comment, final Action prevAction) {
        List<StatusTime> st = new ArrayList<>(list);
        st.set(st.size()-1, updateStatus(st.get(st.size()-1), getTimeDiff(prevAction.getTimestamp(), comment.created)));
        return st;
    }

    public List<StatusTime> getStatusTimeCurrent(final List<StatusTime> list, final Action prevAction, final DateTime endDate) {
        List<StatusTime> st = new ArrayList<>(list);
        st.set(st.size()-1, updateStatus(st.get(st.size()-1), getTimeDiff(prevAction.getTimestamp(), endDate)));
        return st;
    }

    private long getTimeDiff(final DateTime before, final DateTime after) {
        return (after.getMillis() - before.getMillis()) / 1000;
    }
}

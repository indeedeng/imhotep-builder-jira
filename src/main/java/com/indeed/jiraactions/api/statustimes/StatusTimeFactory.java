package com.indeed.jiraactions.api.statustimes;

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

}

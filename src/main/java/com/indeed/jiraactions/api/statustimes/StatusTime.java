package com.indeed.jiraactions.api.statustimes;

import org.immutables.value.Value;

@Value.Immutable
public interface StatusTime {
    String getStatus();
    long getTimeinstatus();
    long getTimetofirst();
    long getTimetolast();
}

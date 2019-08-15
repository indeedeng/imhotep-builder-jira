package com.indeed.jiraactions.api.statustimes;

import org.immutables.value.Value;

@Value.Immutable
public interface StatusTime {
    long getTimeinstatus();
    long getTimetofirst();
    long getTimetolast();
}

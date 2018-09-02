package com.indeed.jiraactions.api.links;

import org.immutables.value.Value;

@Value.Immutable
public interface Link {
    String getTargetKey();
    String getDescription();
}

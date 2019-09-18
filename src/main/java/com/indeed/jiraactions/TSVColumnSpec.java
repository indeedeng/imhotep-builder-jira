package com.indeed.jiraactions;

import org.immutables.value.Value;

import java.util.function.Function;

@Value.Immutable
public interface TSVColumnSpec {
    @Value.Parameter(order = 1)
    String getHeader();
    @Value.Parameter(order = 2)
    Function<Action, String> getActionExtractor();
}

package com.indeed.jiraactions;

import com.google.common.collect.ImmutableList;

import org.immutables.value.Value;

import java.util.List;
import java.util.function.Function;

@Value.Immutable
public interface TSVColumnSpec {
    @Value.Parameter(order = 1)
    String getHeader();
    @Value.Parameter(order = 2)
    Function<Action, String> getActionExtractor();
}

package com.indeed.jiraactions;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

public class Issues {
    private static final Joiner PIPE_JOINER = Joiner.on("|");
    private static final Splitter PIPE_SPLITTER = Splitter.on("|").omitEmptyStrings();

    public static String join(Iterable<?> values) {
        return PIPE_JOINER.join(values);
    }

    public static String join(Object[] values) {
        return PIPE_JOINER.join(values);
    }

    public static Iterable<String> split(final String delimitedString) {
        return PIPE_SPLITTER.split(delimitedString);
    }
}

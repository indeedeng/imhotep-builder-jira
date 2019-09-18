package com.indeed.jiraactions;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.OptionalInt;

public class OutputFormatter {
    private static final Logger log = LoggerFactory.getLogger(OutputFormatter.class);

    static final String TRUNCATED_INDICATOR = "<TRUNCATED>";
    private final OptionalInt maxStringTermLength;

    public OutputFormatter(final JiraActionsIndexBuilderConfig config) {
        this.maxStringTermLength = config.getMaxStringTermLength();

        if (maxStringTermLength.isPresent() && maxStringTermLength.getAsInt() < TRUNCATED_INDICATOR.length()) {
            throw new IllegalArgumentException(String.format(
                            "Must specify a max string term length greater than the length of the truncation indicator \"%s\"",
                            TRUNCATED_INDICATOR));
        }
    }

    @VisibleForTesting
    public OutputFormatter(final OptionalInt maxStringTermLength) {
        this.maxStringTermLength = maxStringTermLength;
    }

    public String truncate(final String source, @Nullable final String delimeter) {
        if (!maxStringTermLength.isPresent()) {
            return source;
        }

        final int maxLength = maxStringTermLength.getAsInt();
        if (source.length() <= maxLength || source.isEmpty()) {
            return source;
        }

        if (delimeter == null || delimeter.isEmpty()) {
            return source.substring(0, maxLength - TRUNCATED_INDICATOR.length()) + TRUNCATED_INDICATOR;
        }

        final StringBuilder sb = new StringBuilder(source);
        while (sb.length() + TRUNCATED_INDICATOR.length() + delimeter.length() > maxLength) {
            final int index = sb.lastIndexOf(delimeter);
            if (index <= 0) { // only one value, remove it
                return TRUNCATED_INDICATOR;
            }

            sb.delete(index, sb.length());
        }

        sb.append(delimeter).append(TRUNCATED_INDICATOR);
        return sb.toString();
    }
}

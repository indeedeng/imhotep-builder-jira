package com.indeed.jiraactions;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public abstract class JiraActionsUtil {
    private JiraActionsUtil() { /* No */ }

    public static final DateTimeZone RAMSES_TIME = DateTimeZone.forOffsetHours(-6);

    // copied from imhotep-builders
    @SuppressWarnings("Duplicates")
    public static DateTime parseDateTime(final String arg) {
        try {
            return new DateTime(arg.trim().replace(" ", "T"), RAMSES_TIME);
        } catch (final IllegalArgumentException ignored) { }
        final DateTime now = new DateTime(RAMSES_TIME);

        switch (arg) {
            case "now":
                return now.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0); // align with start of the hour

            case "today":
                return now.withTimeAtStartOfDay();  // align with start of the day

            case "yesterday":
                return now.minusDays(1).withTimeAtStartOfDay();
            case "tomorrow":
                return now.plusDays(1).withTimeAtStartOfDay();
        }

        final Period p = PeriodParser.parseString(arg);
        if (p != null) {
            DateTime basePoint = new DateTime(RAMSES_TIME);
            final Duration duration = p.toDurationTo(basePoint);

            if(duration.isLongerThan(Duration.standardHours(1))) {
                // align with start of the hour
                basePoint = basePoint.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
            }
            if(duration.isLongerThan(Duration.standardDays(1))) {
                basePoint = basePoint.withTimeAtStartOfDay();
            }
            return basePoint.minus(p);
        }

        throw new IllegalArgumentException("could not parse date: " + arg);
    }

    public static String getUnixTimestamp(@Nullable final DateTime date) {
        if(date == null) {
            return "";
        }
        final long unixTime = date.getMillis()/1000;
        return String.valueOf(unixTime);
    }

    public static long formatDateTimeAsDate(final Optional<DateTime> date) {
        return date.map(JiraActionsUtil::formatDateTimeAsDate).orElse(0L);
    }

    public static long formatDateTimeAsDate(final DateTime date) {
        return Long.parseLong(date.withZone(RAMSES_TIME).toString("yyyyMMdd"));
    }

    public static long formatDateTimeAsTimestamp(final Optional<DateTime> date) {
        return date.map(JiraActionsUtil::formatDateTimeAsTimestamp).orElse(0L);
    }

    public static long formatDateTimeAsTimestamp(final DateTime date) {
        return date.withZone(RAMSES_TIME).getMillis();
    }

    public static String formatDateTimeAsDateTime(final Optional<DateTime> date) {
        return date.map(JiraActionsUtil::formatDateTimeAsDateTime).orElse("");
    }

    public static String formatDateTimeAsDateTime(final DateTime date) {
        return date.withZone(RAMSES_TIME).toString("yyyy-MM-dd HH:mm:ss");
    }

    @Nonnull
    public static String formatStringForIqlField(final String status) {
        if (StringUtils.isEmpty(status)) {
            return "";
        }
        return status
                .toLowerCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace("(", "")
                .replace(")", "")
                .replace("&", "and")
                .replace("/", "_");
    }
}

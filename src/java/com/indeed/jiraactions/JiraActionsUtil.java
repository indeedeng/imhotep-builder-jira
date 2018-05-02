package com.indeed.jiraactions;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class JiraActionsUtil {
    private JiraActionsUtil() { /* No */ }

    public static final DateTimeZone RAMSES_TIME = DateTimeZone.forOffsetHours(-6);

    // copied from imhotep-builders
    @SuppressWarnings("Duplicates")
    @Nonnull
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

    @Nonnull
    public static String getUnixTimestamp(@Nullable final DateTime date) {
        if(date == null) {
            return "";
        }
        final long unixTime = date.getMillis()/1000;
        return String.valueOf(unixTime);
    }
}

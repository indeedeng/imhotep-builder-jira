package com.indeed.skeleton.index.builder.jiraaction;

import com.indeed.imhotep.sql.parser.PeriodParser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;

public class JiraActionUtil {
    private JiraActionUtil() { /* No */ }
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeZone ZONE = DateTimeZone.forOffsetHours(-6);

    // copied from imhotep-builders
    @SuppressWarnings("Duplicates")
    public static DateTime parseDateTime(final String arg) {
        try {
            return new DateTime(arg.trim().replace(" ", "T"), ZONE);
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        final DateTime now = new DateTime(ZONE);

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
            DateTime basePoint = new DateTime(ZONE);
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

    public static String getUnixTimestamp(final String jiraTimestamp) {
        final DateTime date = JiraActionUtil.parseDateTime(jiraTimestamp);
        final long unixTime = date.getMillis()/1000;
        return String.valueOf(unixTime);
    }
}

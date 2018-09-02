package com.indeed.jiraactions;

import com.google.common.base.Strings;
import org.joda.time.Period;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copied from com.indeed.imhotep.sql.parser.PeriodParser
 */
public abstract class PeriodParser {
    private static final Pattern relativeDatePattern = Pattern.compile("(\\s*(\\d+)?\\s*y(?:ear)?s?\\s*,?\\s*)?(\\s*(\\d+)?\\s*mo(?:nth)?s?\\s*,?\\s*)?(\\s*(\\d+)?\\s*w(?:eek)?s?\\s*,?\\s*)?(\\s*(\\d+)?\\s*d(?:ay)?s?\\s*,?\\s*)?(\\s*(\\d+)?\\s*h(?:our)?s?\\s*,?\\s*)?(\\s*(\\d+)?\\s*m(?:inute)?s?\\s*,?\\s*)?(\\s*(\\d+)?\\s*s(?:econd)?s?\\s*)?(?:ago)?\\s*");

    private PeriodParser() { /* No */ }

    @Nullable
    public static Period parseString(final String value) {
        final String cleanedValue = Strings.nullToEmpty(value).toLowerCase();
        final Matcher matcher = relativeDatePattern.matcher(cleanedValue);
        if (!matcher.matches()) {
            return null;
        } else {
            final int years = getValueFromMatch(matcher, 1);
            final int months = getValueFromMatch(matcher, 2);
            final int weeks = getValueFromMatch(matcher, 3);
            final int days = getValueFromMatch(matcher, 4);
            final int hours = getValueFromMatch(matcher, 5);
            final int minutes = getValueFromMatch(matcher, 6);
            final int seconds = getValueFromMatch(matcher, 7);
            return new Period(years, months, weeks, days, hours, minutes, seconds, 0);
        }
    }

    private static int getValueFromMatch(final Matcher matcher, final int i) {
        final String fieldMatch = matcher.group(i * 2 - 1);
        if (Strings.isNullOrEmpty(fieldMatch)) {
            return 0;
        } else {
            final String value = matcher.group(i * 2);
            return tryParseInt(value, 1);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static int tryParseInt(final String val, final int def) {
        int retVal;
        try {
            retVal = Integer.parseInt(val);
        } catch (final NumberFormatException var4) {
            retVal = def;
        }

        return retVal;
    }
}
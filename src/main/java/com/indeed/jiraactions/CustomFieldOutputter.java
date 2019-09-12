package com.indeed.jiraactions;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.customfields.CustomFieldValue;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CustomFieldOutputter {
    private static final Logger log = LoggerFactory.getLogger(CustomFieldValue.class);
    private static final Splitter NUMBER_SPLITTER = Splitter.onPattern("\\D+").omitEmptyStrings();

    private final OutputFormatter outputFormatter;

    public CustomFieldOutputter(final OutputFormatter outputFormatter) {
        this.outputFormatter = outputFormatter;
    }

    public List<String> getValues(final CustomFieldValue customFieldValue) {
        final CustomFieldDefinition definition = customFieldValue.getDefinition();
        final String value = Optional.ofNullable(customFieldValue.getValue()).orElse("");
        final String childValue = Optional.ofNullable(customFieldValue.getChildValue()).orElse("");

        final List<String> values;
        switch(definition.getMultiValueFieldConfiguration()) {
            case NONE:
                values = ImmutableList.of(getTransformedValue(customFieldValue, value));
                break;
            case EXPANDED:
                if(StringUtils.isNotEmpty(value) && StringUtils.isNotEmpty(childValue)) {
                    values = ImmutableList.of(String.format("%s - %s", getTransformedValue(customFieldValue, value), getTransformedValue(customFieldValue, childValue)));
                } else if(StringUtils.isNotEmpty(value)) {
                    values = ImmutableList.of(getTransformedValue(customFieldValue, value));
                } else {
                    values = ImmutableList.of("");
                }
                break;
            default:
                log.error("Unknown multi-field definition {} trying to process field {}",
                        definition.getMultiValueFieldConfiguration(), definition.getName());
                // Intentional fall-through
            case SEPARATE:
            case USERNAME:
                values = ImmutableList.of(getTransformedValue(customFieldValue, value), getTransformedValue(customFieldValue, childValue));
        }

        return values.stream()
                .map(CustomFieldOutputter::sanitize)
                .collect(Collectors.toList());
    }

    private String getTransformedValue(final CustomFieldValue customFieldValue, final String value) {
        final CustomFieldDefinition definition = customFieldValue.getDefinition();

        switch(definition.getTransformation()) {
            case MULTIPLY_BY_THOUSAND:
                return numericStringToMilliNumericString(value);
            case FIRST_NUMBER:
                return findFirstNumber(value);
            case NONE:
                return value;
            default:
                log.error("Unknown transformation {} trying to process field {}",
                        definition.getTransformation(), definition.getName());
                return value;
        }
    }

    private static String sanitize(@Nullable final String value) {
        if(StringUtils.isEmpty(value)) {
            return "";
        }
        return value.replace(Character.toString('\t'), "<tab>");
    }

    public static String numericStringToMilliNumericString(@Nullable final String input) {
        if(StringUtils.isEmpty(input)) {
            return "";
        }
        try {
            final double result = Double.parseDouble(input);
            return String.format("%.0f", result*1000);
        } catch(final NumberFormatException e) {
            log.warn("Failed to convert value {} to milli-value", input, e);
            return "";
        }
    }

    @Nonnull
    public static String findFirstNumber(@Nullable final String input) {
        if(StringUtils.isEmpty(input)) {
            return "";
        }
        final Iterable<String> numberParts = NUMBER_SPLITTER.split(input);
        //noinspection ConstantConditions -- the default value is nonnull
        return Iterables.getFirst(numberParts, "");
    }
}

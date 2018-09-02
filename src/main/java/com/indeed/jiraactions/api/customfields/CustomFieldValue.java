package com.indeed.jiraactions.api.customfields;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import java.util.List;

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class CustomFieldValue {
    private static final Logger log = LoggerFactory.getLogger(CustomFieldValue.class);
    private static final Splitter NUMBER_SPLITTER = Splitter.onPattern("\\D+").omitEmptyStrings();

    private final CustomFieldDefinition definition;
    private final String value;
    private final String childValue;

    protected CustomFieldValue(final CustomFieldDefinition definition, final String value) {
        this(definition, value, "");
    }

    protected CustomFieldValue(final CustomFieldDefinition definition, final String value, final String childValue) {
        this.definition = definition;
        this.value = value;
        this.childValue = childValue;
    }

    protected CustomFieldValue(final CustomFieldDefinition definition) {
        this(definition, "", "");
    }

    protected CustomFieldValue(final CustomFieldValue value) {
        this(value.definition, value.value, value.childValue);
    }

    public static CustomFieldValue emptyCustomField(final CustomFieldDefinition definition) {
        return new CustomFieldValue(definition);
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(String.join("", getValues()));
    }

    public List<String> getValues() {
        switch(definition.getMultiValueFieldConfiguration()) {
            case NONE:
                return ImmutableList.of(sanitize(getTransformedValue(value)));
            case EXPANDED:
                if(StringUtils.isNotEmpty(value) && StringUtils.isNotEmpty(childValue)) {
                    return ImmutableList.of(String.format("%s - %s", sanitize(getTransformedValue(value)), sanitize(getTransformedValue(childValue))));
                } else if(StringUtils.isNotEmpty(value)) {
                    return ImmutableList.of(sanitize(getTransformedValue(value)));
                } else {
                    return ImmutableList.of("");
                }
            default:
                log.error("Unknown multi-field definition {} trying to process field {}",
                        definition.getMultiValueFieldConfiguration(), definition.getName());
                // Intentional fall-through
            case SEPARATE:
            case USERNAME:
                return ImmutableList.of(sanitize(getTransformedValue(value)), sanitize(getTransformedValue(childValue)));
        }
    }

    private String getTransformedValue(final String value) {
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

    @Nonnull
    public static String numericStringToMilliNumericString(@Nullable final String input) {
        if(StringUtils.isEmpty(input)) {
            return "";
        }
        try {
            final double result = Double.parseDouble(input);
            return String.format("%.0f", result*1000);
        } catch(final NumberFormatException e) {
            log.warn("Failed to convert value {} to milli-value", e, input);
            return "";
        }
    }

    @Nonnull
    public static String findFirstNumber(@Nullable final String input) {
        if(StringUtils.isEmpty(input)) {
            return "";
        }
        final Iterable<String> numberParts = NUMBER_SPLITTER.split(input);
        return Iterables.getFirst(numberParts, "");
    }
}

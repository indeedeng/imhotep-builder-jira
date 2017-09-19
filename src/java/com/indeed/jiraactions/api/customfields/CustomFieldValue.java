package com.indeed.jiraactions.api.customfields;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;
import com.indeed.util.logging.Loggers;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class CustomFieldValue {
    private static final Logger log = Logger.getLogger(CustomFieldValue.class);
    private static final Pattern multivaluePattern = Pattern.compile("Parent values: (.*?)\\(\\d+\\)(Level 1 values: (.*?)\\(\\d+\\))?");

    private final CustomFieldDefinition definition;
    private final String value;
    private final String childValue;

    CustomFieldValue(final CustomFieldDefinition definition, final String value) {
        this(definition, value, "");
    }

    @VisibleForTesting
    CustomFieldValue(final CustomFieldDefinition definition, final String value, final String childValue) {
        this.definition = definition;
        this.value = value;
        this.childValue = childValue;
    }

    /**
     * When you're reading the value from the changelog (the Items) instead of the Fields section of the API response.
     * Used for the initial value when it has changed, or when a field has changed throughout the lifetime of an issue.
     * @param value The "to" or "from" value. A keyed representation.
     * @param valueString The "toString" or "fromString" value. A more verbose representation.
     */
    @SuppressWarnings("unused") // Will be used later for more custom things like usernames and links
    public static CustomFieldValue customFieldValueFromChangelog(final CustomFieldDefinition definition,
                                                                 final String value, final String valueString) {
        if(CustomFieldDefinition.MultiValueFieldConfiguration.NONE.equals(definition.getMultiValueFieldConfiguration())) {
            return new CustomFieldValue(definition, valueString, "");
        }

        // Parent values: Escaped bug(20664)Level 1 values: Latent Code Issue(20681)
        final Matcher matcher = multivaluePattern.matcher(valueString);
        final String parent;
        final String child;
        if(matcher.find()) {
            parent = matcher.group(1);
            child = matcher.group(3);
        } else {
            Loggers.error(log, "Unable to parse multi-valued field %s with value %s", definition.getName(), value);
            parent = "";
            child = "";
        }
        return new CustomFieldValue(definition, parent, child);
    }

    public static CustomFieldValue customFieldFromInitialFields(final CustomFieldDefinition definition,
                                                                final JsonNode json) {
        if(CustomFieldDefinition.MultiValueFieldConfiguration.NONE.equals(definition.getMultiValueFieldConfiguration())) {
            final String value = json.asText();
            return new CustomFieldValue(definition, value);
        } else {
            final String value = json.get("value").textValue();
            final JsonNode child = json.get("child");
            if(child == null) {
                return new CustomFieldValue(definition, value);
            } else {
                final String childValue = child.get("value").textValue();
                return new CustomFieldValue(definition, value, childValue);
            }
        }
    }

    public static CustomFieldValue emptyCustomField(final CustomFieldDefinition definition) {
        return new CustomFieldValue(definition, "", "");
    }

    public static CustomFieldValue copyOf(final CustomFieldValue value) {
        return new CustomFieldValue(value.definition, value.value, value.childValue);
    }

    public CustomFieldDefinition getDefinition() {
        return definition;
    }

    @SuppressWarnings("ConstantConditions")
    public void writeValue(final Writer writer) throws IOException {
        if(CustomFieldDefinition.MultiValueFieldConfiguration.NONE.equals(definition.getMultiValueFieldConfiguration())) {
            writer.write(sanitize(getTransformedValue(value)));
        } else {
            if (CustomFieldDefinition.MultiValueFieldConfiguration.EXPANDED.equals(definition.getMultiValueFieldConfiguration())) {
                if(StringUtils.isNotEmpty(value) && StringUtils.isNotEmpty(childValue)) {
                    writer.write(String.format("%s - %s", sanitize(getTransformedValue(value)), sanitize(getTransformedValue(childValue))));
                } else if(StringUtils.isNotEmpty(value)) {
                    writer.write(sanitize(getTransformedValue(value)));
                } else {
                    writer.write("");
                }
            } else if(CustomFieldDefinition.MultiValueFieldConfiguration.SEPARATE.equals(definition.getMultiValueFieldConfiguration())) {
                writer.write(String.format("%s\t%s", sanitize(getTransformedValue(value)), sanitize(getTransformedValue(childValue))));
            } else {
                Loggers.error(log, "Unknown multi-field definition %s trying to process field %s",
                        definition.getMultiValueFieldConfiguration(), definition.getName());
            }
        }
    }

    private String getTransformedValue(final String value) {
        switch(definition.getTransformation()) {
            case MULTIPLY_BY_THOUSAND:
                return numericStringToMilliNumericString(value);
            case NONE:
                return value;
            default:
                Loggers.error(log, "Unknown transformation %s trying to process field %s",
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
            Loggers.warn(log, "Failed to convert value %s to milli-value", e, input);
            return "";
        }
    }
}

package com.indeed.jiraactions.api.customfields;

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


    public CustomFieldValue(final CustomFieldDefinition definition, final String value) {
        this.definition = definition;
        this.value = value;
    }

    public void writeValue(final Writer writer) throws IOException {
        if(CustomFieldDefinition.MultiValueFieldConfiguration.NONE.equals(definition.getMultiValueFieldConfiguration())) {
            writer.write(sanitize(getTransformedValue(value)));
        } else {
            // Parent values: Escaped bug(20664)Level 1 values: Latent Code Issue(20681)
            final Matcher matcher = multivaluePattern.matcher(value);
            final String parent;
            final String child;
            if(matcher.find()) {
                parent = sanitize(getTransformedValue(matcher.group(1)));
                child = sanitize(getTransformedValue(matcher.group(3)));
            } else {
                parent = null;
                child = null;
            }

            if (CustomFieldDefinition.MultiValueFieldConfiguration.EXPANDED.equals(definition.getMultiValueFieldConfiguration())) {
                if(parent != null && child != null) {
                    writer.write(String.format("%s - %s", parent, child));
                } else if(parent != null) {
                    writer.write(parent);
                } else {
                    Loggers.error(log, "Unable to parse multi-valued field %s with value %s", definition.getName(), value);
                    writer.write("");
                }
            } else if (CustomFieldDefinition.MultiValueFieldConfiguration.SEPARATE.equals(definition.getMultiValueFieldConfiguration())) {
                if(parent != null && child != null) {
                    writer.write(String.format("%s\t%s", parent, child));
                } else if(parent != null) {
                    writer.write(String.format("%s\t", parent));
                } else {
                    Loggers.error(log, "Unable to parse multi-valued field %s with value %s", definition.getName(), value);
                    writer.write("\t");
                }
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

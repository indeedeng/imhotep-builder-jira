package com.indeed.jiraactions.api.customfields;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.commons.lang.StringUtils;
import org.immutables.value.Value;

import java.io.IOException;
import java.io.Writer;

@Value.Immutable
@JsonSerialize(as = ImmutableCustomFieldDefinition.class)
@JsonDeserialize(as = ImmutableCustomFieldDefinition.class)
public interface CustomFieldDefinition {
    /* This is a bit of a kludge. When Jackson 2.9 comes out, it adds direct support to do this:
     * https://stackoverflow.com/a/44217670/1515497
     */
    enum MultiValueFieldConfiguration {
        SEPARATE,
        EXPANDED,
        USERNAME, // This is kind of a kludge because it doesn't fit the other types of multi-values, but it keeps the model clean
        NONE;

        @JsonCreator
        public static MultiValueFieldConfiguration fromString(final String key) {
            return StringUtils.isEmpty(key) ? NONE : MultiValueFieldConfiguration.valueOf(key.toUpperCase());
        }
    }

    enum Transformation {
        MULTIPLY_BY_THOUSAND,
        FIRST_NUMBER,
        NONE;

        @JsonCreator
        public static Transformation fromString(final String key) {
            return StringUtils.isEmpty(key) ? NONE : Transformation.valueOf(key.toUpperCase());
        }
    }

    enum SplitRule {
        NON_NUMBER("\\D+"),
        NONE("");

        private final String splitPattern;

        SplitRule(final String splitPattern) {
            this.splitPattern = splitPattern;
        }

        public String getSplitPattern() {
            return splitPattern;
        }

        @JsonCreator
        public static SplitRule fromString(final String key) {
            return StringUtils.isEmpty(key) ? NONE : SplitRule.valueOf(key.toUpperCase());
        }
    }

    String getName();
    String[] getCustomFieldId();
    String getImhotepFieldName();

    @Value.Default
    default String getSeparator() {
        return "";
    }

    @Value.Default
    default SplitRule getSplit() {
        return SplitRule.NONE;
    }

    @Value.Default
    default String[] getAlternateNames() {
        return new String[] {};
    }

    @Value.Default
    default MultiValueFieldConfiguration getMultiValueFieldConfiguration() {
        return MultiValueFieldConfiguration.NONE;
    }

    @Value.Default
    default Transformation getTransformation() {
        return Transformation.NONE;
    }

    default void writeHeader(final Writer writer) throws IOException {
        switch(getMultiValueFieldConfiguration()) {
            case SEPARATE:
                writer.write(String.format("%s\t%s", getImhotepFieldName()+"1", getImhotepFieldName()+"2"));
                break;
            case USERNAME:
                writer.write(String.format("%s\t%s", getImhotepFieldName(), getImhotepFieldName()+"username"));
                break;
            case EXPANDED:
            default:
                writer.write(getImhotepFieldName());
                break;
        }
    }
}

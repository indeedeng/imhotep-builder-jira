package com.indeed.jiraactions.api.customfields;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(defaultAsDefault = true)
@JsonSerialize(as = ImmutableCustomFieldDefinition.class)
@JsonDeserialize(as = ImmutableCustomFieldDefinition.class)
public interface CustomFieldDefinition {
    enum MultiValueFieldConfiguration {
        /** Split result into two fields (suffixed 1 and 2) */
        SEPARATE,
        EXPANDED,
        /** Expand username values to name and username */
        USERNAME, // This is kind of a kludge because it doesn't fit the other types of multi-values, but it keeps the model clean
        /** Expand string values in ISO date-time format into three fields for yyyyMMdd, yyyyMMddHHmmss, and timestamp */
        DATETIME, // Equally kludgy to USERNAME. Consider refactoring.

        @JsonEnumDefaultValue
        NONE;
    }

    enum Transformation {
        MULTIPLY_BY_THOUSAND,
        FIRST_NUMBER,

        @JsonEnumDefaultValue
        NONE;
    }

    enum SplitRule {
        NON_NUMBER("\\D+"),

        @JsonEnumDefaultValue
        NONE("");

        private final String splitPattern;

        SplitRule(final String splitPattern) {
            this.splitPattern = splitPattern;
        }

        public String getSplitPattern() {
            return splitPattern;
        }
    }

    SplitConfig EMPTY_SPLIT_CONFIG = ImmutableSplitConfig.builder().build();
    @Value.Immutable
    @Value.Style(defaultAsDefault = true)
    @JsonSerialize(as = ImmutableSplitConfig.class)
    @JsonDeserialize(as = ImmutableSplitConfig.class)
    interface SplitConfig {
        default boolean removeEmptyStrings() {
            return false;
        }
    }

    String getName();
    String[] getCustomFieldId();
    String getImhotepFieldName();

    default String getSeparator() {
        return "";
    }

    default SplitRule getSplit() {
        return SplitRule.NONE;
    }

    String[] EMPTY_ARRAY = new String[0];
    default String[] getAlternateNames() {
        return EMPTY_ARRAY;
    }

    default MultiValueFieldConfiguration getMultiValueFieldConfiguration() {
        return MultiValueFieldConfiguration.NONE;
    }

    default Transformation getTransformation() {
        return Transformation.NONE;
    }

    default SplitConfig getSplitConfig() {
        return EMPTY_SPLIT_CONFIG;
    }

    default List<String> getHeaders() {
        switch(getMultiValueFieldConfiguration()) {
            case SEPARATE:
                return ImmutableList.of(getImhotepFieldName() + "1", getImhotepFieldName() + "2");
            case USERNAME:
                return ImmutableList.of(getImhotepFieldName(), getImhotepFieldName() + "username");
            case DATETIME:
                return ImmutableList.of(
                        "int "+ getImhotepFieldName() + "date",
                        "string "+ getImhotepFieldName() + "datetime",
                        "int "+ getImhotepFieldName() + "timestamp");
            case EXPANDED:
            default:
                return ImmutableList.of(getImhotepFieldName());
        }
    }
}

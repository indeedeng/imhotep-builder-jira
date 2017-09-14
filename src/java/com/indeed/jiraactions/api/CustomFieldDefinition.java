package com.indeed.jiraactions.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang.StringUtils;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableCustomFieldDefinition.class)
@JsonDeserialize(as = ImmutableCustomFieldDefinition.class)
public interface CustomFieldDefinition {
    /* This is a bit of a kludge. When Jackson 2.9 comes out, it adds direct support to do this:
     * https://stackoverflow.com/a/44217670/1515497
     */
    enum MultiValueFieldConfiguration {
        SEPARATE("separate"),
        EXPANDED("expanded"),
        NONE("none");

        private final String key;

        MultiValueFieldConfiguration(final String key) {
            this.key = key;
        }

        @JsonCreator
        public static MultiValueFieldConfiguration fromString(final String key) {
            return StringUtils.isEmpty(key) ? NONE : MultiValueFieldConfiguration.valueOf(key.toUpperCase());
        }

        @JsonValue
        public String getKey() {
            return key;
        }
    }

    enum Transformation {
        MULTIPLY_BY_THOUSAND("multiply_by_thousand"),
        NONE("none");

        private final String key;

        Transformation(final String key) {
            this.key = key;
        }

        @JsonCreator
        public static Transformation fromString(final String key) {
            return StringUtils.isEmpty(key) ? NONE : Transformation.valueOf(key.toUpperCase());
        }

        @JsonValue
        public String getKey() {
            return key;
        }
    }

    String getName();
    String getCustomFieldId();
    String getImhotepFieldName();

    @Value.Default
    default MultiValueFieldConfiguration getMultiValueFieldConfiguration() {
        return MultiValueFieldConfiguration.NONE;
    }

    @Value.Default
    default Transformation getTransformation() {
        return Transformation.NONE;
    }

}

package com.indeed.jiraactions.api.customfields;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang.StringUtils;
import org.immutables.value.Value;

import javax.annotation.Nullable;
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
        SEPARATE("separate"),
        EXPANDED("expanded"),
        USERNAME("username"), // This is kind of a kludge because it doesn't fit the other types of multi-values, but it keeps the model clean
        NONE("none");

        private final String key;

        MultiValueFieldConfiguration(final String key) {
            this.key = key;
        }

        @JsonCreator
        public static MultiValueFieldConfiguration fromString(final String key) {
            return StringUtils.isEmpty(key) ? NONE : MultiValueFieldConfiguration.valueOf(key.toUpperCase());
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
    }

    String getName();
    String getCustomFieldId();
    String getImhotepFieldName();

    @Nullable
    String getSeparator();

    @Value.Default
    default String getAlternateName() {
        return "";
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

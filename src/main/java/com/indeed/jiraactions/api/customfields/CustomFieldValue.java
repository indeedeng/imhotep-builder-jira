package com.indeed.jiraactions.api.customfields;

import org.apache.commons.lang.StringUtils;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
public interface CustomFieldValue {
    @Value.Parameter
    CustomFieldDefinition getDefinition();

    @Nullable String getValue();

    @Nullable String getChildValue();

    @Value.Lazy
    default boolean isEmpty() {
        return StringUtils.isBlank(getValue());
    }
}

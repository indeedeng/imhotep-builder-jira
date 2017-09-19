package com.indeed.jiraactions.api.customfields;

import com.google.common.annotations.VisibleForTesting;
import com.indeed.jiraactions.api.response.issue.Issue;
import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class CustomFieldApiParser {
    public static CustomFieldValue parseInitialValue(final CustomFieldDefinition definition, final Issue issue) {
        final String label = getItemLabel(definition.getName());
        final Item item = issue.initialItem(label, true);
        if(item != null) {
            return CustomFieldValue.customFieldValueFromChangelog(definition, item.from, item.fromString);
        } else {
            return null;
        }
    }

    @VisibleForTesting
    static String getItemLabel(final String name) {
        return name.toLowerCase().replace(" ", "-");
    }
}

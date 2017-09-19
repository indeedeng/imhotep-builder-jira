package com.indeed.jiraactions.api.customfields;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.indeed.jiraactions.Action;
import com.indeed.jiraactions.api.response.issue.Issue;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;
import com.indeed.util.logging.Loggers;
import org.apache.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class CustomFieldApiParser {
    private static final Logger log = Logger.getLogger(CustomFieldApiParser.class);

    public static CustomFieldValue parseInitialValue(final CustomFieldDefinition definition, final Issue issue) {
        final String label = getItemLabel(definition.getName());
        final Item item = issue.initialItem(label, true);
        if(item != null) {
            return CustomFieldValue.customFieldValueFromChangelog(definition, item.from, item.fromString);
        } else {
            final JsonNode jsonNode = issue.fields.getCustomField(definition.getCustomFieldId());
            if(jsonNode == null) {
                return CustomFieldValue.emptyCustomField(definition);
            } else {
                return CustomFieldValue.customFieldFromInitialFields(definition, jsonNode);
            }
        }
    }

    public static CustomFieldValue parseNonInitialValue(final CustomFieldDefinition definition, final Action prevAction,
                                                        final History history) {
        final String label = getItemLabel(definition.getName());
        final Item item = history.getItem(label, true);
        if(item == null) {
            final Optional<CustomFieldValue> prevValue = prevAction.getCustomFieldValues().stream().filter(x -> definition.equals(x.getDefinition())).findFirst();
            if(!prevValue.isPresent()) {
                Loggers.error(log,"No previous value for %s found for issue %s.", definition.getName(), prevAction.getIssuekey());
                return CustomFieldValue.emptyCustomField(definition);
            } else {
                return CustomFieldValue.copyOf(prevValue.get());
            }
        } else {
            return CustomFieldValue.customFieldValueFromChangelog(definition, item.to, item.toString);
        }
    }

    @VisibleForTesting
    static String getItemLabel(final String name) {
        return name.toLowerCase().replace(" ", "-");
    }
}

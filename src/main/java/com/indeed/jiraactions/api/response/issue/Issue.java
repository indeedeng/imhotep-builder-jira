package com.indeed.jiraactions.api.response.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.indeed.jiraactions.Issues;
import com.indeed.jiraactions.api.response.issue.changelog.ChangeLog;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;
import com.indeed.jiraactions.api.response.issue.fields.Field;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)
@SuppressWarnings("CanBeFinal")
public class Issue {
    public String key;
    public Field fields;
    public ChangeLog changelog;

    // Multivalued rich object fields are treated different in Jira changelog histories than
    //  multivalued primitive fields.
    private static final Set<String> MULTIVALUED_RICH_FIELDS = ImmutableSet.of(
            "component",
            "fixversions"
    );

    public String initialValue(final String field) throws IOException {
        return initialValue(field, field);
    }

    /**
     * Determines the initial value of the field based on current issue state and recorded changelog.
     */
    public String initialValue(final String field, final String fallbackField) throws IOException {
        if (MULTIVALUED_RICH_FIELDS.contains(field)) {
            // Multivalued rich fields' initial state must be determined by inference from
            //  taking the current state and walking the history to reverse the actions.
            final List<String> values =
                    Strings.isNullOrEmpty(this.fields.getStringValue(field)) ?
                            Lists.newArrayList() :
                            Lists.newArrayList(Issues.split(this.fields.getStringValue(field)));

            if (null != this.changelog && null != this.changelog.histories) {
                for (int i = this.changelog.histories.length - 1; i >= 0; i--) {
                    final History history = this.changelog.histories[i];
                    for (final Item item: history.getAllItems(field)) {
                        // Reverse the action.
                        if (null == item.to) {
                            // Reverse the removal
                            values.add(item.fromString);
                        } else {
                            // Reverse an addition
                            values.remove(item.toString);
                        }
                    }
                }
            }

            return Issues.join(values);

        } else {
            // Single- and multi-valued primitive fields' initial state can be determined by examining
            //  the current state and first history item alone, since multivalued values are fully
            //  recorded as a delimited string.
            final Item history = initialItem(false, field);
            if(history != null) {
                return history.fromString == null ? "" : history.fromString;
            } else {
                return this.fields.getStringValue(fallbackField);
            }
        }
    }

    public String initialValueKey(final String field, final String fallbackField) throws IOException {
        final Item history = initialItem(false, field);
        if(history != null) {
            return history.from == null ? "" : history.from;
        } else {
            return this.fields.getStringValue(fallbackField);
        }
    }

    @Nullable
    public Item initialItem(final boolean acceptCustom, final String... fields) {
        return this.changelog.getFirstHistoryItem(acceptCustom, fields);
    }

    public String toString() {
        return key;
    }
}

package com.indeed.jiraactions.api.customfields;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.annotations.VisibleForTesting;
import com.indeed.common.util.StringUtils;
import com.indeed.jiraactions.Action;
import com.indeed.jiraactions.UserLookupService;
import com.indeed.jiraactions.api.response.issue.Issue;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;
import com.indeed.util.logging.Loggers;
import org.apache.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class CustomFieldApiParser {
    private static final Logger log = Logger.getLogger(CustomFieldApiParser.class);
    private static final Pattern multivaluePattern = Pattern.compile("Parent values: (.*?)\\(\\d+\\)(Level 1 values: (.*?)\\(\\d+\\))?");

    private final UserLookupService userLookupService;

    public CustomFieldApiParser(final UserLookupService userLookupService) {
        this.userLookupService = userLookupService;
    }

    public CustomFieldValue parseInitialValue(final CustomFieldDefinition definition, final Issue issue) {
        final Item item = issue.initialItem(true, getItemLabels(definition));
        if(item != null) {
            return customFieldValueFromChangelog(definition, item.from, item.fromString);
        } else {
            final JsonNode jsonNode = issue.fields.getCustomField(definition.getCustomFieldId());
            if(jsonNode == null) {
                return new CustomFieldValue(definition);
            } else {
                return customFieldFromInitialFields(definition, jsonNode);
            }
        }
    }

    public CustomFieldValue parseNonInitialValue(final CustomFieldDefinition definition, final Action prevAction,
                                                        final History history) {
        final Item item = history.getItem(true, getItemLabels(definition));
        if(item != null) {
            return customFieldValueFromChangelog(definition, item.to, item.toString);
        } else {
            final CustomFieldValue prevValue = prevAction.getCustomFieldValues().get(definition);
            if(prevValue == null) {
                Loggers.error(log,"No previous value for %s found for issue %s.", definition.getName(), prevAction.getIssuekey());
                return new CustomFieldValue(definition);
            } else {
                return new CustomFieldValue(prevValue);
            }
        }
    }

    /**
     * When you're reading the value from the changelog (the Items) instead of the Fields section of the API response.
     * Used for the initial value when it has changed, or when a field has changed throughout the lifetime of an issue.
     * @param value The "to" or "from" value. A keyed representation.
     * @param valueString The "toString" or "fromString" value. A more verbose representation.
     */
    CustomFieldValue customFieldValueFromChangelog(final CustomFieldDefinition definition,
                                                   final String value, final String valueString) {
        if(CustomFieldDefinition.MultiValueFieldConfiguration.NONE.equals(definition.getMultiValueFieldConfiguration())) {
            return new CustomFieldValue(definition, valueString, "");
        } else if(CustomFieldDefinition.MultiValueFieldConfiguration.USERNAME.equals(definition.getMultiValueFieldConfiguration())) {
            final User user = userLookupService.getUser(value);
            return new CustomFieldValue(definition, valueString, user.name);
        } else {

            if (StringUtils.isEmpty(valueString)) {
                return new CustomFieldValue(definition, "", "");
            }

            // Parent values: Escaped bug(20664)Level 1 values: Latent Code Issue(20681)
            final Matcher matcher = multivaluePattern.matcher(valueString);
            final String parent;
            final String child;
            if (matcher.find()) {
                parent = matcher.group(1);
                child = matcher.group(3);
            } else {
                Loggers.error(log, "Unable to parse multi-valued field %s with value %s", definition.getName(), value);
                parent = "";
                child = "";
            }
            return new CustomFieldValue(definition, parent, child);
        }
    }

    CustomFieldValue customFieldFromInitialFields(final CustomFieldDefinition definition,
                                                  final JsonNode json) {
        if(CustomFieldDefinition.MultiValueFieldConfiguration.NONE.equals(definition.getMultiValueFieldConfiguration())) {
            final String value = getValueFromNode(definition, json);
            return new CustomFieldValue(definition, value);
        } else if(CustomFieldDefinition.MultiValueFieldConfiguration.USERNAME.equals(definition.getMultiValueFieldConfiguration())) {
            final String username = getValueFromNode(definition, json.get("name"));
            final String displayName = getValueFromNode(definition, json.get("displayName"));
            final User user = userLookupService.getUser(username);
            return new CustomFieldValue(definition, displayName, user.name);
        } else {
            final String value = getValueFromNode(definition, json);
            final JsonNode child = json.get("child");
            if(child == null) {
                return new CustomFieldValue(definition, value);
            } else {
                final String childValue = getValueFromNode(definition, child);
                return new CustomFieldValue(definition, value, childValue);
            }
        }
    }

    private static String getValueFromNode(final CustomFieldDefinition definition, final JsonNode node) {
        if(JsonNodeType.ARRAY.equals(node.getNodeType())) {
            final Iterator<JsonNode> children = node.elements();
            if(children == null) {
                return "";
            }
            final Iterable<JsonNode> iterable = () -> children;
            final Iterable<String> values = () -> StreamSupport.stream(iterable.spliterator(), false).map(x -> getValueFromNode(definition, x)).iterator();
            return String.join(definition.getSeparator(), values);
        } else {
            if(node.has("value")) {
                return node.get("value").asText();
            } else {
                final String text = node.asText();
                if (text.startsWith("com.atlassian.greenhopper.service")) { // This is a hack since we don't have the source code or JAR for greenhopper
                    final int index = text.indexOf("name=");
                    if (index < 0) {
                        return text;
                    }
                    final int start = index + "name=".length();
                    final int end = text.indexOf(",", start);
                    return text.substring(start, end >= start ? end : text.length());
                } else {
                    return text;
                }
            }
        }
    }


    @VisibleForTesting
    static String getItemLabel(final String name) {
        return name.toLowerCase().replace(" ", "-");
    }

    static String[] getItemLabels(final CustomFieldDefinition definition) {
        final String label = getItemLabel(definition.getName());

        if (StringUtils.isEmpty(definition.getAlternateName())) {
            return new String[] { label };
        } else {
            final String alternateLabel = getItemLabel(definition.getAlternateName());
            return new String[] { label, alternateLabel };
        }
    }
}

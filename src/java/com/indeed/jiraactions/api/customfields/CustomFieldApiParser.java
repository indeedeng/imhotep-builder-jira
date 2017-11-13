package com.indeed.jiraactions.api.customfields;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class CustomFieldApiParser {
    private static final Logger log = Logger.getLogger(CustomFieldApiParser.class);
    private static final Pattern MULTIVALUE_PATTERN = Pattern.compile("Parent values: (.*?)\\(\\d+\\)(Level 1 values: (.*?)\\(\\d+\\))?");

    private final UserLookupService userLookupService;
    private final Set<CustomFieldDefinition> failedCustomFields;
    private final Set<CustomFieldDefinition> failedCustomHistoryFields;

    public CustomFieldApiParser(final UserLookupService userLookupService) {
        this.userLookupService = userLookupService;
        this.failedCustomFields = new HashSet<>();
        this.failedCustomHistoryFields = new HashSet<>();
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
                final CustomFieldValue customFieldValue = customFieldFromInitialFields(definition, jsonNode);
                if (StringUtils.isEmpty(customFieldValue.getFormattedValue())) {
                    if (!failedCustomFields.contains(definition)) {
                        log.debug(String.format("Customfield %s failed to parse json node %s", definition, jsonNode));
                        failedCustomFields.add(definition);
                    }
                }
                return customFieldValue;
            }
        }
    }

    public CustomFieldValue parseNonInitialValue(final CustomFieldDefinition definition, final Action prevAction,
                                                 final History history) {
        final Item item = history.getItem(true, getItemLabels(definition));
        if(item != null) {
            final CustomFieldValue value = customFieldValueFromChangelog(definition, item.to, item.toString);
            if (StringUtils.isNotEmpty(item.toString) && StringUtils.isEmpty(value.getFormattedValue())) {
                if (!failedCustomHistoryFields.contains(definition)) {
                    log.debug(String.format("Customfield %s failed to parse history item %s", definition, item.toString));
                    failedCustomHistoryFields.add(definition);
                }
            }
            return value;
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
    CustomFieldValue customFieldValueFromChangelog(
            final CustomFieldDefinition definition,
            final String value,
            final String valueString
    ) {
        final boolean valueStringIsEmpty = StringUtils.isEmpty(valueString);
        final String splitValueString;
        final boolean shouldSplit =
                StringUtils.isNotEmpty(definition.getSplit())
                && StringUtils.isNotEmpty(definition.getSeparator())
                && !valueStringIsEmpty;
        if (shouldSplit) {
            splitValueString = valueString.replaceAll(definition.getSplit(), definition.getSeparator());
        } else {
            splitValueString = valueString;
        }
        if(CustomFieldDefinition.MultiValueFieldConfiguration.NONE.equals(definition.getMultiValueFieldConfiguration())) {
            if (StringUtils.isNotEmpty(definition.getSeparator()) && !valueStringIsEmpty) {
                return new CustomFieldValue(definition, splitValueString.replaceAll(", ?", definition.getSeparator()), "");
            } else {
                return new CustomFieldValue(definition, splitValueString, "");
            }
        } else if(CustomFieldDefinition.MultiValueFieldConfiguration.USERNAME.equals(definition.getMultiValueFieldConfiguration())) {
            final String usernames;
            if (shouldSplit) {
                final ImmutableList.Builder<String> usernameList = ImmutableList.builder();
                for (final String userKey : Splitter.on(definition.getSeparator()).split(splitValueString)) {
                    final User user = userLookupService.getUser(userKey);
                    usernameList.add(user.name);
                }
                usernames = Joiner.on(definition.getSeparator()).join(usernameList.build());
            } else {
                final User user = userLookupService.getUser(value);
                usernames = user.name;
            }
            final User user = userLookupService.getUser(value);
            return new CustomFieldValue(definition, splitValueString, usernames);
        } else {

            if (valueStringIsEmpty) {
                return new CustomFieldValue(definition, "", "");
            }

            // Parent values: Escaped bug(20664)Level 1 values: Latent Code Issue(20681)
            final Matcher matcher = MULTIVALUE_PATTERN.matcher(splitValueString);
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
            final String username = getValueFromNode(definition, json.get("key"));
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
            final String separator;
            if(StringUtils.isEmpty(definition.getSeparator())) {
                Loggers.error(log, "No specified separator for array node %s for field %s", node.toString(), definition.getName());
                separator = " ";
            } else {
                separator = definition.getSeparator();
            }
            final Iterator<JsonNode> children = node.elements();
            if(children == null) {
                return "";
            }
            final Iterable<JsonNode> iterable = () -> children;
            final Iterable<String> values = () -> StreamSupport.stream(iterable.spliterator(), false).map(x -> getValueFromNode(definition, x)).iterator();
            return String.join(separator, values);
        } else {
            if(node.has("value")) {
                final String nodeValue = node.get("value").asText();
                if (StringUtils.isNotEmpty(definition.getSplit()) && StringUtils.isNotEmpty(definition.getSeparator())) {
                    return nodeValue.replaceAll(definition.getSplit(), definition.getSeparator());
                } else {
                    return nodeValue;
                }
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
                } else if (StringUtils.isNotEmpty(definition.getSplit()) && StringUtils.isNotEmpty(definition.getSeparator())) {
                    return text.replaceAll(definition.getSplit(), definition.getSeparator());
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

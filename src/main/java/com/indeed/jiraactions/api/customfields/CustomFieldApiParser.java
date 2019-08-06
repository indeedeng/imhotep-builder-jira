package com.indeed.jiraactions.api.customfields;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.indeed.jiraactions.Action;
import com.indeed.jiraactions.UserLookupService;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition.SplitRule;
import com.indeed.jiraactions.api.response.issue.Issue;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class CustomFieldApiParser {
    private static final Logger log = LoggerFactory.getLogger(CustomFieldApiParser.class);
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
            final Optional<JsonNode> firstFound = Arrays.stream(definition.getCustomFieldId())
                    .map(id -> issue.fields.getCustomField(id))
                    .filter(Objects::nonNull)
                    .filter(x -> !"null".equals(x.asText())).findFirst();
            return firstFound.map(jsonNode -> {
                final CustomFieldValue customFieldValue = customFieldFromInitialFields(definition, jsonNode);
                if (customFieldValue.isEmpty()) {
                    if (!failedCustomFields.contains(definition)) {
                        log.debug("Customfield {} failed to parse json node {}", definition, jsonNode);
                        failedCustomFields.add(definition);
                    }
                }
                return customFieldValue;
            }).orElseGet(() -> new CustomFieldValue(definition));
        }
    }

    public CustomFieldValue parseNonInitialValue(final CustomFieldDefinition definition, final Action prevAction,
                                                 final History history) {
        final Item item = history.getItem(true, getItemLabels(definition));
        if(item != null) {
            final CustomFieldValue value = customFieldValueFromChangelog(definition, item.to, item.toString);
            if (StringUtils.isNotEmpty(item.toString) && value.isEmpty()) {
                if (!failedCustomHistoryFields.contains(definition)) {
                    log.debug("Customfield {} failed to parse history item {}", definition, item.toString);
                    failedCustomHistoryFields.add(definition);
                }
            }
            return value;
        } else {
            final CustomFieldValue prevValue = prevAction.getCustomFieldValues().get(definition);
            if(prevValue == null) {
                log.error("No previous value for {} found for issue {}.", definition.getName(), prevAction.getIssuekey());
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
                definition.getSplit() != SplitRule.NONE
                && StringUtils.isNotEmpty(definition.getSeparator())
                && !valueStringIsEmpty;
        final String splitPattern;
        if (shouldSplit) {
            splitPattern = definition.getSplit().getSplitPattern();
            splitValueString = valueString.replaceAll(
                    splitPattern,
                    definition.getSeparator()
            );
        } else {
            splitPattern = "";
            splitValueString = valueString;
        }
        if(CustomFieldDefinition.MultiValueFieldConfiguration.NONE.equals(definition.getMultiValueFieldConfiguration())) {
            if (StringUtils.isNotEmpty(definition.getSeparator()) && !valueStringIsEmpty) {
                final String split = StringUtils.isEmpty(splitPattern) ? ", ?" : splitPattern;
                return new CustomFieldValue(definition, splitValueString.replaceAll(split, definition.getSeparator()), "");
            } else {
                return new CustomFieldValue(definition, splitValueString, "");
            }
        } else if(CustomFieldDefinition.MultiValueFieldConfiguration.USERNAME.equals(definition.getMultiValueFieldConfiguration())) {
            final String usernames;
            final String groups;
            if (shouldSplit) {
                final ImmutableList.Builder<String> usernameList = ImmutableList.builder();
                final List<String> groupsList = new ArrayList<>();
                for (final String userKey : Splitter.on(definition.getSeparator()).split(splitValueString)) {
                    final User user = userLookupService.getUser(userKey);
                    usernameList.add(user.getName());
                    groupsList.add(String.join("|", user.getGroups()));
                }
                usernames = Joiner.on(definition.getSeparator()).join(usernameList.build());
                groups = String.join("|", groupsList);
            } else {
                final User user = userLookupService.getUser(value);
                usernames = user.getName();
                groups = String.join("|", user.getGroups());
            }
            return new CustomFieldValue(definition, splitValueString, usernames, groups);
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
                log.error("Unable to parse multi-valued field {} with value {}", definition.getName(), value);
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
            final String groups = String.join("|", user.getGroups());
            return new CustomFieldValue(definition, displayName, user.getName(), groups);
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
            final List<JsonNode> children = ImmutableList.copyOf(node.elements());

            final String separator;
            if(StringUtils.isEmpty(definition.getSeparator())) {
                if(children.size() > 1) {
                    log.error("No specified separator for multi-valued field {}, array node: {}", definition.getName(), node.toString());
                } else {
                    log.warn("No specified separator for field {} with an array of one element", definition.getName());
                }
                separator = " ";
            } else {
                separator = definition.getSeparator();
            }

            final Iterable<String> values = children.stream().map(x -> getValueFromNode(definition, x))::iterator;
            return String.join(separator, values);
        } else {
            if(node.has("value")) {
                final String nodeValue = node.get("value").asText();
                if (definition.getSplit() != SplitRule.NONE && StringUtils.isNotEmpty(definition.getSeparator())) {
                    return nodeValue.replaceAll(definition.getSplit().getSplitPattern(), definition.getSeparator());
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
                } else if (definition.getSplit() != SplitRule.NONE && StringUtils.isNotEmpty(definition.getSeparator())) {
                    return text.replaceAll(definition.getSplit().getSplitPattern(), definition.getSeparator());
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

        if (ArrayUtils.isEmpty(definition.getAlternateNames())) {
            return new String[] { label };
        } else {
            return ImmutableList.<String>builder()
                    .add(label)
                    .addAll(Arrays.stream(definition.getAlternateNames()).map(CustomFieldApiParser::getItemLabel)::iterator)
                    .build().toArray(new String[0]);
        }
    }
}

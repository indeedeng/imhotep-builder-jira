package com.indeed.jiraactions;

import com.google.common.collect.ImmutableList;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.customfields.CustomFieldValue;
import com.indeed.jiraactions.api.customfields.ImmutableCustomFieldValue;
import com.indeed.jiraactions.api.links.Link;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.jiraactions.api.statustimes.StatusTime;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TSVSpecBuilder {
    private static final Logger log = LoggerFactory.getLogger(TSVSpecBuilder.class);
    private static final Pattern SPACE_PATTERN = Pattern.compile(" ", Pattern.LITERAL);

    private final ImmutableList.Builder<TSVColumnSpec> columnSpecs = ImmutableList.builder();

    private final OutputFormatter outputFormatter;
    private final CustomFieldOutputter customFieldOutputter;

    public TSVSpecBuilder(final OutputFormatter outputFormatter,
                          final CustomFieldOutputter customFieldOutputter) {
        this.outputFormatter = outputFormatter;
        this.customFieldOutputter = customFieldOutputter;
    }

    public List<TSVColumnSpec> build() {
        return columnSpecs.build();
    }

    public TSVSpecBuilder addColumn(final String header, final Function<Action, String> valueExtractor) {
        columnSpecs.add(ImmutableTSVColumnSpec.of(header, valueExtractor));
        return this;
    }

    public TSVSpecBuilder addUserColumns(final String header, final Function<Action, User> userExtractor) {
        addColumn(header, action -> userExtractor.apply(action).getDisplayName());
        addColumn(header + "username", action -> userExtractor.apply(action).getName());
        addColumn(header + "groups*|", action -> String.join("|", userExtractor.apply(action).getGroups()));
        return this;
    }

    public TSVSpecBuilder addTimeColumn(final String header, final Function<Action, DateTime> timeExtractor) {
        addColumn(header, action -> JiraActionsUtil.getUnixTimestamp(timeExtractor.apply(action)));
        return this;
    }

    public TSVSpecBuilder addLongColumn(final String header, final Function<Action, Long> longExtractor) {
        addColumn(header, action -> String.valueOf(longExtractor.apply(action)));
        return this;
    }

    public TSVSpecBuilder addStatusTimeColumns(final List<String> statusTypes) {
        for (final String type : statusTypes) {
            final Function<Action, Long> totalStatusTime = action -> getTotalStatusTime(type, action.getStatusTimes());
            final Function<Action, Long> timeToFirst = action -> getTimeToFirst(type, action.getStatusTimes());
            final Function<Action, Long> timeToLast = action -> getTimeToLast(type, action.getStatusTimes());
            final String formattedType = JiraActionsUtil.formatStringForIqlField(type);
            addLongColumn(String.format("totaltime_%s", formattedType), totalStatusTime);
            addLongColumn(String.format("timetofirst_%s", formattedType), timeToFirst);
            addLongColumn(String.format("timetolast_%s", formattedType), timeToLast);
        }
        final Function<Action, String> valueExtractor = TSVSpecBuilder::getAllStatuses;
        addColumn("statushistory*|", valueExtractor);

        return this;
    }

    public TSVSpecBuilder addCustomFieldColumns(final CustomFieldDefinition customField) {
        final List<String> headers = customField.getHeaders();
        final Function<Action, List<String>> valueExtractor = action -> getCustomFieldValue(customField, action);
        for (int i = 0; i < headers.size(); i++) {
            final int index = i; // must be final for the lambda expression
            addColumn(headers.get(index), action -> valueExtractor.apply(action).get(index));
        }
        return this;
    }

    public TSVSpecBuilder addLinkColumns(final List<String> linkTypes) {
        for (final String type : linkTypes) {
            final Function<Action, String> valueExtractor = action -> getLinkValue(type, action);
            addColumn(
                    String.format("link_%s*", SPACE_PATTERN.matcher(type).replaceAll(Matcher.quoteReplacement("_"))), valueExtractor);
        }

        final Function<Action, String> valueExtractor = this::getAllLinksValue;
        addColumn("links*", valueExtractor);

        return this;
    }

    private List<String> getCustomFieldValue(final CustomFieldDefinition customField, final Action action) {
        final CustomFieldValue value = action.getCustomFieldValues().get(customField);
        if (value == null) {
            log.error(
                    "No value found for custom field {} for issue {}",
                    customField.getImhotepFieldName(),
                    action.getIssuekey()
            );
            return customFieldOutputter.getValues(ImmutableCustomFieldValue.of(customField));
        } else {
            return customFieldOutputter.getValues(value);
        }
    }

    private String getLinkValue(final String linkType, final Action action) {
        final String delimeter = " ";
        final String links = action.getLinks().stream()
                .filter(x -> x.getDescription().equals(linkType))
                .map(Link::getTargetKey)
                .collect(Collectors.joining(delimeter));

        return outputFormatter.truncate(links, delimeter);
    }

    private String getAllLinksValue(final Action action) {
        final String delimeter = " ";
        final String links = action.getLinks().stream()
                .map(Link::getTargetKey)
                .collect(Collectors.joining(delimeter));

        return outputFormatter.truncate(links, delimeter);
    }

    private static long getTotalStatusTime(final String statusType, final Map<String, StatusTime> statusTimeMap) {
        if (statusTimeMap.containsKey(statusType)) {
            return statusTimeMap.get(statusType).getTimeinstatus();
        }
        return 0;
    }

    private static long getTimeToFirst(final String statusType, final Map<String, StatusTime> statusTimeMap) {
        if (statusTimeMap.containsKey(statusType)) {
            return statusTimeMap.get(statusType).getTimetofirst();
        }
        return 0;
    }

    private static long getTimeToLast(final String statusType, final Map<String, StatusTime> statusTimeMap) {
        if (statusTimeMap.containsKey(statusType)) {
            return statusTimeMap.get(statusType).getTimetolast();
        }
        return 0;
    }

    private static String getAllStatuses(final Action action) {
        return String.join("|", action.getStatusHistory());
    }
}

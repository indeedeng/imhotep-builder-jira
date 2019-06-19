package com.indeed.jiraactions;

import com.google.common.collect.ImmutableList;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.customfields.CustomFieldValue;
import com.indeed.jiraactions.api.links.Link;
import com.indeed.jiraactions.api.response.issue.User;

import com.indeed.jiraactions.api.statustimes.StatusTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import java.util.List;
import java.util.function.Function;

public class TSVSpecBuilder {
    private static final Logger log = LoggerFactory.getLogger(TSVSpecBuilder.class);

    private final ImmutableList.Builder<TSVColumnSpec> columnSpecs;

    public TSVSpecBuilder() {
        columnSpecs = ImmutableList.builder();
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

    public TSVSpecBuilder addIntColumn(final String header, final Function<Action, Integer> intExtractor) {
        addColumn(header, action -> String.valueOf(intExtractor.apply(action)));
        return this;
    }

    public TSVSpecBuilder addCustomFieldColumns(final CustomFieldDefinition customField) {
        final List<String> headers = customField.getHeaders();
        final Function<Action, List<String>> valueExtractor = action -> getCustomFieldValue(customField, action);
        for (int i = 0; i < headers.size(); i++) {
            final int index = i;
            addColumn(headers.get(index), action -> valueExtractor.apply(action).get(index));
        }
        return this;
    }

    public TSVSpecBuilder addStatusTimeColumns(final List<String> statusTypes) {
        for(final String type : statusTypes) {
            final Function<Action, Long> totalStatusTime = action -> getTotalStatusTime(type, action);
            final Function<Action, Long> timeToFirst = action -> getTimeToFirst(type, action);
            final Function<Action, Long> timeToLast = action -> getTimeToLast(type, action);
            String formattedType = type.toLowerCase()
                    .replace(" ", "_")
                    .replace("-", "_")
                    .replace("(", "")
                    .replace(")", "")
                    .replace("&", "and")
                    .replace("/", "_");
            addLongColumn(String.format("totaltime_%s", formattedType), totalStatusTime);
            addLongColumn(String.format("timetofirst_%s", formattedType), timeToFirst);
            addLongColumn(String.format("timetolast_%s", formattedType), timeToLast);
        }
        final Function<Action, String> valueExtractor = TSVSpecBuilder::getAllStatuses;
        addColumn("statushistory", valueExtractor);

        return this;
    }

    public TSVSpecBuilder addLinkColumns(final List<String> linkTypes) {
        for(final String type : linkTypes) {
            final Function<Action, String> valueExtractor = action -> getLinkValue(type, action);
            addColumn(
                    String.format("link_%s*", type.replace(" ", "_")), valueExtractor);
        }

        final Function<Action, String> valueExtractor = TSVSpecBuilder::getAllLinksValue;
        addColumn("links*", valueExtractor);

        return this;
    }

    private static List<String> getCustomFieldValue(final CustomFieldDefinition customField, final Action action) {
        final CustomFieldValue value = action.getCustomFieldValues().get(customField);
        if (value == null) {
            log.error(
                    "No value found for custom field {} for issue {}",
                    customField.getImhotepFieldName(),
                    action.getIssuekey()
            );
            return CustomFieldValue.emptyCustomField(customField).getValues();
        } else {
            return value.getValues();
        }
    }

    private static String getLinkValue(final String linkType, final Action action) {
        final Iterable<String> values = action.getLinks().stream()
                .filter(x -> x.getDescription().equals(linkType))
                .map(Link::getTargetKey)::iterator;

        return String.join(" ", values);
    }

    private static String getAllLinksValue(final Action action) {
        final Iterable<String> values = action.getLinks().stream()
                .map(Link::getTargetKey)::iterator;

        return String.join(" ", values);
    }

    private static long getTotalStatusTime(final String statusType, final Action action) {
        final List<StatusTime> st = action.getStatustimes();
        long output = 0;
        for(StatusTime statusTime : st) {
            if (statusTime.getStatus().equals(statusType)) {
                output = output + statusTime.getTimeinstatus();
            }
        }
        return output;
    }
    private static long getTimeToFirst(final String statusType, final Action action) {
        final List<StatusTime> st = action.getStatustimes();
        long output = 0;
        for(StatusTime statusTime : st) {
            if (statusTime.getStatus().equals(statusType)) {
                output = output + statusTime.getTimetofirst();
            }
        }
        return output;
    }
    private static long getTimeToLast(final String statusType, final Action action) {
        final List<StatusTime> st = action.getStatustimes();
        long output = 0;
        for(StatusTime statusTime : st) {
            if (statusTime.getStatus().equals(statusType)) {
                output = output + statusTime.getTimetolast();
            }
        }
        return output;
    }

    private static String getAllStatuses(final Action action) {
        final Iterable<String> values = action.getStatustimes().stream()
                .map(StatusTime::getStatus)::iterator;

        return String.join(", ", values);
    }


}

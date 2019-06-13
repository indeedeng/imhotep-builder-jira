package com.indeed.jiraactions;

import com.google.common.annotations.VisibleForTesting;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.customfields.CustomFieldValue;
import com.indeed.jiraactions.api.links.Link;
import com.indeed.jiraactions.api.response.issue.User;
import org.apache.commons.lang.StringUtils;
import org.immutables.value.Value;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

@Value.Immutable
public interface Action {
    String getIssuekey();
    User getActor();
    User getAssignee();
    long getIssueage();
    String getIssuetype();
    String getProject();
    String getProjectkey();
    User getReporter();
    String getResolution();
    String getStatus();
    String getSummary();
    DateTime getTimestamp();
    long getTimeinstatus();
    String getCategory();
    String getFixversions();
    String getDueDate();
    String getComponents();
    String getLabels();
    String getCreatedDate();
    String getPriority();
    int getComments();
    String getDateResolved();
    String getDateClosed();
    Map<CustomFieldDefinition, CustomFieldValue> getCustomFieldValues();
    Set<Link> getLinks();

    @Nullable
    @VisibleForTesting
    @Value.Derived
    default DateTime getDueDateTime() {
        if (StringUtils.isEmpty(getDueDate())) {
            return null;
        }
        return JiraActionsUtil.parseDateTime(getDueDate()).plusDays(1).withTimeAtStartOfDay();
    }

    @Value.Derived
    default boolean isInRange(final DateTime start, final DateTime end) {
        return start.compareTo(getTimestamp()) <= 0 && end.compareTo(getTimestamp()) > 0;
    }
    @Value.Derived
    default boolean isBefore(final DateTime date) {
        return date.compareTo(getTimestamp()) >= 0;
    }
}

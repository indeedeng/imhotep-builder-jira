package com.indeed.jiraactions;

import com.google.common.annotations.VisibleForTesting;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.customfields.CustomFieldValue;
import com.indeed.jiraactions.api.links.Link;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.jiraactions.api.statustimes.StatusTime;
import org.apache.commons.lang.StringUtils;
import org.immutables.value.Value;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.List;

@Value.Immutable
public interface Action {
    String getAction();
    User getActor();
    User getAssignee();
    String getFieldschanged();
    long getIssueage();
    String getIssuekey();
    String getIssuetype();
    String getProject();
    String getProjectkey();
    String getPrevstatus();
    User getReporter();
    String getResolution();
    String getStatus();
    String getSummary();
    long getTimeinstate();
    long getTimesinceaction();
    DateTime getTimestamp();
    String getCategory();
    String getFixversions();
    String getDueDate();
    String getComponents();
    String getLabels();
    String getCreatedDate();
    int getCreatedDateInt();
    int getClosedDate();
    int getResolvedDate();
    int getLastUpdated();
    long getDlt();
    String getPriority();
    int getComments();
    List<StatusTime> getStatustimes();
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
}

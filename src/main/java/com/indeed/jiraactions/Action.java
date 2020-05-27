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
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    List<String> getFixVersions();
    String getDueDate();
    List<String> getComponents();
    String getLabels();

    /**
     * @return Date issue was created, in yyyy-MM-dd format
     */
    String getCreatedDate();

    /**
     * Returns an integer value that can be used for date arithmetic.
     *
     * @return Long integer representing the date the issue was created, as represented in yyyyMMdd format.
     */
    long getCreatedDateLong();

    /**
     * Returns an integer value that can be used for date-time arithmetic.
     *
     * @return Long integer representing the date the issue was created, as represented in yyyyMMddhhmmss format.
     */
    long getCreatedDateTimeLong();

    /**
     * Returns an integer value that can be used for date arithmetic.
     *
     * @return Long integer representing the date the issue was created, in Unix epoch time format
     */
    long getCreatedDateTimestamp();

    /**
     * @return Date issue was created, in yyyy-MM-dd format
     */
    String getResolutionDate();

    /**
     * Returns an integer value that can be used for date arithmetic.
     *
     * @return Long integer representing the date the issue was resolved, as represented in yyyyMMdd format.
     */
    long getResolutionDateLong();

    /**
     * Returns an integer value that can be used for date-time arithmetic.
     *
     * @return Long integer representing the date the issue was resolved, as represented in yyyyMMddhhmmss format.
     */
    long getResolutionDateTimeLong();

    /**
     * Returns an integer value that can be used for date arithmetic.
     *
     * @return Long integer representing the date the issue was resolved, in Unix epoch time format
     */
    long getResolutionDateTimestamp();

    long getTimeOriginalEstimate();
    long getTimeEstimate();
    long getTimeSpent();

    String getPriority();
    Map<CustomFieldDefinition, CustomFieldValue> getCustomFieldValues();
    Set<Link> getLinks();
    long getClosedDate();
    long getLastUpdated();
    long getDeliveryLeadTime();
    long getComments();
    Map<String, StatusTime> getStatusTimes();
    List<String> getStatusHistory();

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
    default String getCreatedDateTime() {
        return new DateTime(getCreatedDateLong()).withZone(DateTimeZone.forOffsetHours(-6)).toString("yyyy-MM-dd HH:mm:ss");
    }

    @Value.Derived
    default String getResolutionDateTime() {
        return new DateTime(getResolutionDateTimeLong()).withZone(DateTimeZone.forOffsetHours(-6)).toString("yyyy-MM-dd HH:mm:ss");
    }

    @Value.Derived
    default String getFixVersionsJoined() {
        return Issues.join(getFixVersions());
    }

    @Value.Derived
    default String getComponentsJoined() {
        return Issues.join(getComponents());
    }
}

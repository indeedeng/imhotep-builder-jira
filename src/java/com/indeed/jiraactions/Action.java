package com.indeed.jiraactions;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.immutables.value.Value;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

@Value.Immutable
public interface Action {
    String getAction();

    String getActor();

    String getActorusername();

    String getAssignee();

    String getAssigneeusername();

    String getFieldschanged();

    long getIssueage();

    String getIssuekey();

    String getIssuetype();

    String getProject();

    String getProjectkey();

    String getPrevstatus();

    String getReporter();

    String getReporterusername();

    String getResolution();

    String getStatus();

    String getSummary();

    long getTimeinstate();

    long getTimesinceaction();

    DateTime getTimestamp();

    String getVerifier();

    String getVerifierusername();

    String getCategory();

    String getFixversions();

    String getDueDate();

    String getComponents();

    String getLabels();

    String getIssueSizeEstimate();

    String getDirectCause();

    String getSprints();

    @Nullable
    @VisibleForTesting
    @Value.Derived
    default DateTime getDueDateTime() {
        if (StringUtils.isEmpty(getDueDate())) {
            return null;
        }
        return JiraActionsUtil.parseDateTime(getDueDate()).plusDays(1).withTimeAtStartOfDay();
    }
}

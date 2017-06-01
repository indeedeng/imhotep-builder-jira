package com.indeed.jiraactions;

import org.immutables.value.Value;
import org.joda.time.DateTime;

@Value.Immutable
interface Action {
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
}

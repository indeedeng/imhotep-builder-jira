package com.indeed.jiraactions;

import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;

import org.immutables.value.Value;

import java.util.Set;

@Value.Immutable
public interface JiraActionsIndexBuilderConfig {
    String getJiraUsername();
    String getJiraPassword();
    String getJiraBaseURL();
    String getJiraFields();
    String getJiraExpand();
    String getJiraProject();
    String getExcludedJiraProject();
    String getIuploadURL();
    String getIuploadUsername();
    String getIuploadPassword();
    String getStartDate();
    String getEndDate();
    int getJiraBatchSize();
    String getIndexName();
    boolean buildJiraIssues();
    String getJiraIssuesDownloadUrl();
    String getJiraIssuesUploadUrl();
    int getJiraIssuesRange();
    Set<String> getDeliveryLeadTimeStatuses();
    Set<String> getDeliveryLeadTimeResolutions();
    Set<String> getDeliveryLeadTimeTypes();
    CustomFieldDefinition[] getCustomFields();
}

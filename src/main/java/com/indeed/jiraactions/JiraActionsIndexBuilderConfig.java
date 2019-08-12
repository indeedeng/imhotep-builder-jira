package com.indeed.jiraactions;

import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;

import org.immutables.value.Value;

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
    int getJiraIssuesRange();
    String getDeliveryLeadTimeFields();
    CustomFieldDefinition[] getCustomFields();
}

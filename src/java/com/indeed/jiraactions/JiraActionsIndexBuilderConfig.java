package com.indeed.jiraactions;

import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;

import org.immutables.value.Value;

@Value.Immutable
public interface JiraActionsIndexBuilderConfig {
    public String getJiraUsername();
    public String getJiraPassword();
    public String getJiraBaseURL();
    public String getJiraFields();
    public String getJiraExpand();
    public String getJiraProject();
    public String getExcludedJiraProject();
    public String getIuploadURL();
    public String getIuploadUsername();
    public String getIuploadPassword();
    public String getStartDate();
    public String getEndDate();
    public int getJiraBatchSize();
    public String getIndexName();
    public CustomFieldDefinition[] getCustomFields();
}

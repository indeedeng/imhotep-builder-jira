package com.indeed.skeleton.index.builder.jiraaction;

public class JiraActionIndexBuilderConfig {
    private final String jiraUsername;
    private final String jiraPassword;

    private final String jiraBaseURL;
    private final String jiraFields;
    private final String jiraExpand;
    private final String jiraProject;
    private final String excludedJiraProject;

    private final String iuploadURL;
    private final String iuploadUsername;
    private final String iuploadPassword;

    private final String indexName;

    private final String startDate;
    private final String endDate;

    private final int jiraBatchSize;

    private final boolean ignoreCustomFields;

    public JiraActionIndexBuilderConfig(final String jiraUsername, final String jiraPassword, final String jiraUrl,
                                        final String jiraFields, final String jiraExpand, final String jiraProject,
                                        final String excludedJiraProject, final String iuploadUrl,
                                        final String iuploadUsername, final String iuploadPassword,
                                        final String startDate, final String endDate, final int jiraBatchSize,
                                        final String indexName, final boolean ignoreCustomFields) {
        this.jiraUsername = jiraUsername;
        this.jiraPassword = jiraPassword;
        this.jiraBaseURL = jiraUrl;
        this.jiraFields = jiraFields;
        this.jiraExpand = jiraExpand;
        this.jiraProject = jiraProject;
        this.excludedJiraProject = excludedJiraProject;
        this.iuploadURL = iuploadUrl;
        this.iuploadUsername = iuploadUsername;
        this.iuploadPassword = iuploadPassword;
        this.startDate = startDate;
        this.endDate = endDate;
        this.jiraBatchSize = jiraBatchSize;
        this.indexName = indexName;
        this.ignoreCustomFields = ignoreCustomFields;
    }

    public String getJiraUsername() {
        return jiraUsername;
    }

    public String getJiraPassword() {
        return jiraPassword;
    }

    public String getJiraBaseURL() {
        return jiraBaseURL;
    }

    public String getJiraFields() {
        return jiraFields;
    }

    public String getJiraExpand() {
        return jiraExpand;
    }

    public String getJiraProject() {
        return jiraProject;
    }

    public String getExcludedJiraProject() {
        return excludedJiraProject;
    }

    public String getIuploadURL() {
        return iuploadURL;
    }

    public String getIuploadUsername() {
        return iuploadUsername;
    }

    public String getIuploadPassword() {
        return iuploadPassword;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public int getJiraBatchSize() {
        return jiraBatchSize;
    }

    public String getIndexName() {
        return indexName;
    }

    public boolean isIgnoreCustomFields() {
        return ignoreCustomFields;
    }
}

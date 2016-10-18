package com.indeed.skeleton.index.builder.jiraaction;

public class JiraActionIndexBuilderConfig {
    private final String jiraUsernameIndexer;
    private final String jiraPasswordIndexer;

    private final String jiraBaseURL;
    private final String jiraFields;
    private final String jiraExpand;
    private final String jiraProject;

    private final String iuploadURL;

    public JiraActionIndexBuilderConfig(final String jiraUsername, final String jiraPassword, final String jiraUrl,
                                       final String jiraFields, final String jiraExpand, final String jiraProject,
                                       final String iuploadUrl) {
        this.jiraUsernameIndexer = jiraUsername;
        this.jiraPasswordIndexer = jiraPassword;
        this.jiraBaseURL = jiraUrl;
        this.jiraFields = jiraFields;
        this.jiraExpand = jiraExpand;
        this.jiraProject = jiraProject;
        this.iuploadURL = iuploadUrl;
    }

    public String getJiraUsernameIndexer() {
        return jiraUsernameIndexer;
    }

    public String getJiraPasswordIndexer() {
        return jiraPasswordIndexer;
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

    public String getIuploadURL() {
        return iuploadURL;
    }
}

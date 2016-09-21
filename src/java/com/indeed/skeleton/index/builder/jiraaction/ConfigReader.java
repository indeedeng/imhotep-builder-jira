package com.indeed.skeleton.index.builder.jiraaction;

/**
 * Created by soono on 9/8/16.
 */
interface ConfigReader {
    public String username();
    public String password();
    public String jiraBaseURL();
    public String jiraFields();
    public String jiraExpand();
    public String jiraProject();

    public String iuploadURL();
}
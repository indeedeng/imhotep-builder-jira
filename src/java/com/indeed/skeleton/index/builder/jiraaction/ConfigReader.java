package com.indeed.skeleton.index.builder.jiraaction;

/**
 * Created by soono on 9/8/16.
 */
interface ConfigReader {
    public String jiraUser();
    public String jiraPass();
    public String jiraBaseURL();
    public String apiFields();
    public String apiExpand();
    public String apiProject();

    public String iuploadURL();
    public String iuploadUser();
    public String iuploadPass();
}
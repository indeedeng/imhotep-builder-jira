package com.indeed.skeleton.index.builder.jiraaction;

/**
 * Created by soono on 9/8/16.
 */
interface ConfigReader {
    String username();
    String password();
    String jiraBaseURL();
    String jiraFields();
    String jiraExpand();
    String jiraProject();

    String iuploadURL();
}
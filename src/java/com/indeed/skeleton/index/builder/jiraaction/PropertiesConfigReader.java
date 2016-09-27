package com.indeed.skeleton.index.builder.jiraaction;

/**
 * Created by soono on 9/8/16.
 */
public class PropertiesConfigReader implements ConfigReader {
    @Override
    public String username() {
        return Config.jiraUsernameIndexer;
    }

    @Override
    public String password() {
        return Config.jiraPasswordIndexer;
    }

    @Override
    public String jiraBaseURL() {
        return Config.jiraBaseURL;
    }

    @Override
    public String jiraFields() {
        return Config.jiraFields.substring(1, Config.jiraFields.length()-1).replaceAll("\\s+","");
    }

    @Override
    public String jiraExpand() {
        return Config.jiraExpand;
    }

    @Override
    public String jiraProject() {
        return Config.jiraProject;
    }

    @Override
    public String iuploadURL() {
        return Config.iuploadURL;
    }
}

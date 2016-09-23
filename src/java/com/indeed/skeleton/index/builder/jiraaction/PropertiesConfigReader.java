package com.indeed.skeleton.index.builder.jiraaction;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by soono on 9/8/16.
 */
public class PropertiesConfigReader implements ConfigReader {
    public static final Properties prop = new Properties();
    private static final String propFileName = "config.properties";

    static {
        InputStream inputStream;
        inputStream = PropertiesConfigReader.class.getClassLoader().getResourceAsStream(propFileName);

        if (inputStream != null) {
            try {
                prop.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        Config.jiraUsernameIndexer = PropertiesConfigReader.prop.getProperty("jira.username.indexer"); // fixme: remove
//        Config.jiraPasswordIndexer = PropertiesConfigReader.prop.getProperty("jira.password.indexer"); // fixme: remove
//        Config.jiraBaseURL = PropertiesConfigReader.prop.getProperty("jira.baseurl");
//        Config.jiraFields = PropertiesConfigReader.prop.getProperty("jira.fields");
//        Config.jiraExpand = PropertiesConfigReader.prop.getProperty("jira.expand");
//        Config.jiraProject = PropertiesConfigReader.prop.getProperty("jira.project");
//
//        Config.iuploadURL = PropertiesConfigReader.prop.getProperty("iupload.url");
    }

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

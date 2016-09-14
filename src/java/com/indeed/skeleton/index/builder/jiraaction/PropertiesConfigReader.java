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

        Config.jiraUser = PropertiesConfigReader.prop.getProperty("jiraUsername");
        Config.jiraPass = PropertiesConfigReader.prop.getProperty("jiraPassword");
        Config.jiraBaseURL = PropertiesConfigReader.prop.getProperty("jiraBaseURL");
        Config.apiFields = PropertiesConfigReader.prop.getProperty("jiraFields");
        Config.apiExpand = PropertiesConfigReader.prop.getProperty("jiraExpand");
        Config.apiProject = PropertiesConfigReader.prop.getProperty("jiraProject");

        Config.iuploadURL = PropertiesConfigReader.prop.getProperty("iuploadURL");
        Config.iuploadUser = PropertiesConfigReader.prop.getProperty("iuploadUser");
        Config.iuploadPass = PropertiesConfigReader.prop.getProperty("iuploadPass");
    }

    @Override
    public String jiraUser() {
        return Config.jiraUser;
    }

    @Override
    public String jiraPass() {
        return Config.jiraPass;
    }

    @Override
    public String jiraBaseURL() {
        return Config.jiraBaseURL;
    }

    @Override
    public String apiFields() {
        return Config.apiFields;
    }

    @Override
    public String apiExpand() {
        return Config.apiExpand;
    }

    @Override
    public String apiProject() {
        return Config.apiProject;
    }

    @Override
    public String iuploadURL() {
        return Config.iuploadURL;
    }

    @Override
    public String iuploadUser() {
        return Config.iuploadUser;
    }

    @Override
    public String iuploadPass() {
        return Config.iuploadPass;
    }
}

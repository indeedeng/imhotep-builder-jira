package com.indeed.skeleton.index.builder.jiraaction;

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

        Config.username = PropertiesConfigReader.prop.getProperty("jiraUsername");
        Config.password = PropertiesConfigReader.prop.getProperty("jiraPassword");
        Config.baseURL = PropertiesConfigReader.prop.getProperty("jiraBaseURL");
        Config.apiFields = PropertiesConfigReader.prop.getProperty("jiraFields");
        Config.apiExpand = PropertiesConfigReader.prop.getProperty("jiraExpand");
    }

    @Override
    public String username() {
        return Config.username;
    }

    @Override
    public String password() {
        return Config.password;
    }

    @Override
    public String baseURL() {
        return Config.baseURL;
    }

    @Override
    public String apiFields() {
        return Config.apiFields;
    }

    @Override
    public String apiExpand() {
        return Config.apiExpand;
    }
}

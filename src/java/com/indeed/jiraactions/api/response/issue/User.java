package com.indeed.jiraactions.api.response.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class User {
    public static final User INVALID_USER;
    static {
        INVALID_USER = new User();
        INVALID_USER.displayName = "No User";
        INVALID_USER.name = "";
        INVALID_USER.key = "";
    }
    public String displayName;
    public String name;
    public String key;
}

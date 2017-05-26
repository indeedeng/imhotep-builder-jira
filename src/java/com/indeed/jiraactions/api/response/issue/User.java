package com.indeed.jiraactions.api.response.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author soono
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class User {
    public String displayName;
    public String key;
}

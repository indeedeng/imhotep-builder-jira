package com.indeed.jiraactions.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.indeed.jiraactions.JiraActionsIndexBuilderConfig;
import com.indeed.jiraactions.UserLookupService;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ConcurrentHashMap;


@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class ApiUserLookupService implements UserLookupService {
    private static final Logger log = Logger.getLogger(ApiUserLookupService.class);
    private static final String API_BASE = "/rest/api/2/user";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final ApiCaller apiCaller;
    private final String baseUrl;
    private long userLookupTime;

    public ApiUserLookupService(final JiraActionsIndexBuilderConfig config, final ApiCaller apiCaller) {
        this.apiCaller = apiCaller;

        baseUrl = config.getJiraBaseURL() + API_BASE;
    }

    @Override
    public User getUser(@Nullable final String key) {
        if(StringUtils.isEmpty(key)) {
            return User.NOBODY;
        }

        if(!users.containsKey(key)) {
            final User user = lookupUser(key);
            users.put(key, user);
        }

        return users.get(key);
    }

    public int numLookups() {
        return users.size();
    }

    private String getApiUrlForUser(final String key) throws UnsupportedEncodingException {
        return baseUrl + "?expand=groups&key=" + URLEncoder.encode(key, "UTF-8");
    }

    public long getUserLookupTotalTime() {
        return userLookupTime;
    }

    private User lookupUser(final String key) {
        final long start = System.currentTimeMillis();

        try {
            final String url = getApiUrlForUser(key);
            final JsonNode json = apiCaller.getJsonNode(url);
            return parseUser(json);
        } catch(final IOException e) {
            log.error("Could not find user " + key + ". Using fallback.", e);
            return User.getFallbackUser(key);
        } finally {
            final long end = System.currentTimeMillis();
            userLookupTime += (end - start);
        }
    }

    @VisibleForTesting
    static User parseUser(final JsonNode json) throws JsonProcessingException {
        return objectMapper.treeToValue(json, User.class);
    }
}

package com.indeed.jiraactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.common.util.StringUtils;
import com.indeed.jiraactions.JiraActionsIndexBuilderConfig;
import com.indeed.jiraactions.UserLookupService;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class ApiUserLookupService extends ApiCaller implements UserLookupService {
    private static final Logger log = Logger.getLogger(ApiUserLookupService.class);
    private static final String API_BASE = "/rest/api/2/user";

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;
    private long userLookupTime;

    public ApiUserLookupService(final JiraActionsIndexBuilderConfig config) {
        super(config);

        baseUrl = config.getJiraBaseURL() + API_BASE;
    }

    @Override
    public User getUser(@Nullable final String key) throws IOException {
        if(StringUtils.isEmpty(key)) {
            return User.INVALID_USER;
        }

        if(!users.containsKey(key)) {
            users.put(key, lookupUser(key));
        }

        return users.get(key);
    }

    public int numLookups() {
        return users.size();
    }

    private String getApiUrlForUser(final String key) throws UnsupportedEncodingException {
        return baseUrl + "?key=" + URLEncoder.encode(key, "UTF-8");
    }

    public long getUserLookupTotalTime() {
        return userLookupTime;
    }

    private User lookupUser(final String key) throws IOException {
        final long start = System.currentTimeMillis();

        final String url = getApiUrlForUser(key);
        final JsonNode json = getJsonNode(url);
        final User user = objectMapper.treeToValue(json, User.class);

        final long end = System.currentTimeMillis();
        userLookupTime += (end - start);
        log.trace(String.format("Took %d milliseconds to look up user.%s", (end - start),
                Objects.equals(key, user.name) ? "" : " They had a different username than key."));
        return user;
    }
}

package com.indeed.jiraactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.jiraactions.JiraActionsIndexBuilderConfig;
import org.apache.commons.codec.binary.Base64;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public abstract class ApiCaller {
    protected final JiraActionsIndexBuilderConfig config;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String authentication;

    public ApiCaller(final JiraActionsIndexBuilderConfig config) {
        this.config = config;
        this.authentication = getBasicAuth();

    }

    protected JsonNode getJsonNode(final String url) throws IOException {
        final HttpsURLConnection urlConnection = getURLConnection(url);
        final InputStream in = urlConnection.getInputStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            final String apiRes = br.readLine();
            return objectMapper.readTree(apiRes);
        }
    }

    private HttpsURLConnection getURLConnection(final String urlString) throws IOException {
        final URL url = new URL(urlString);
        final HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Authorization", authentication);
        return urlConnection;
    }

    private String getBasicAuth() {
        final String userPass = config.getJiraUsername() + ":" + config.getJiraPassword();
        final String basicAuth = "Basic " + new String(new Base64().encode(userPass.getBytes()));
        return basicAuth;
    }
}

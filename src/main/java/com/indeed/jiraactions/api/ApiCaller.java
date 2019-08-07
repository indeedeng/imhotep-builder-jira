package com.indeed.jiraactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.jiraactions.JiraActionsIndexBuilderConfig;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ApiCaller {
    protected final JiraActionsIndexBuilderConfig config;

    private static final Logger log = LoggerFactory.getLogger(IssuesAPICaller.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client;
    private final String authentication;

    private String cookies = "";
    private String pinnedNode = null;

    public ApiCaller(final JiraActionsIndexBuilderConfig config) {
        this.config = config;
        this.authentication = getBasicAuth();
        this.client = new OkHttpClient.Builder().build();
    }

    public JsonNode getJsonNode(final String url) throws IOException {
        final Request request = new Request.Builder()
                .header("Authorization", this.authentication)
                .header("Cache-Control", "no-store")
                .header("Cookie", cookies)
                .url(url)
                .build();
        final Response response = client.newCall(request).execute();

        final String anodeId = response.header("X-ANODEID");

        if(!Objects.equals(pinnedNode, anodeId)) {
            if (pinnedNode != null) {
                log.warn("Expected X-ANODEID={} but found {}", pinnedNode, anodeId);
            }
            cookies = String.join(";", response.headers("Set-Cookie"));
            pinnedNode = anodeId;
        }
        try (final ResponseBody responseBody = response.body()){

            if (!response.isSuccessful()) {
                final StringBuilder sb = new StringBuilder();
                final Headers requestHeaders = request.headers();
                final Headers responseHeaders = response.headers();

                sb.append('{');
                sb.append("\"Request\": {");
                sb.append("\"URL\": \"").append(url).append("\",");

                sb.append("\"Headers\": {");
                for (int i = 0, size = requestHeaders.size(); i < size; i++) {
                    final String key = requestHeaders.name(i);
                    final String value;
                    if ("Authorization".equals(key)) {
                        value = "<Omitted>";
                    } else {
                        value = String.join(",", requestHeaders.value(i));
                    }
                    sb.append("\"").append(key).append("\": \"").append(value).append("\",");
                }
                sb.append('}');
                sb.append('}');

                sb.append(", \"Response\": {");
                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                    final String key = responseHeaders.name(i);
                    final String value;
                    if ("Set-Cookie".equals(key)) {
                        value = "<Omitted>";
                    } else {
                        value = String.join(",", responseHeaders.value(i));
                    }
                    sb.append("\"").append(key).append("\": \"").append(value).append("\",");
                }
                sb.append("\"Code\": ").append(response.code()).append(",");
                sb.append("\"Message\": \"").append(response.message()).append("\",");
                sb.append("\"Error Body\": \"").append(responseBody.string()).append("\"");
                sb.append('}');
                sb.append('}');
                throw new IOException(String.valueOf(response));
            }
            return objectMapper.readTree(responseBody.string());
        } catch (IOException e) {
            throw e;
        }
    }

    private String getBasicAuth() {
        final String userPass = config.getJiraUsername() + ":" + config.getJiraPassword();
        final String basicAuth = "Basic " + new String(new Base64().encode(userPass.getBytes()));
        return basicAuth;
    }

}

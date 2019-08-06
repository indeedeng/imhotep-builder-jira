package com.indeed.jiraactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.jiraactions.JiraActionsIndexBuilderConfig;

import okhttp3.ConnectionPool;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiCaller {
    protected final JiraActionsIndexBuilderConfig config;

    private static final Logger log = LoggerFactory.getLogger(ApiCaller.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private OkHttpClient client;
    private final String authentication;

    public ApiCaller(final JiraActionsIndexBuilderConfig config) {
        this.config = config;
        this.authentication = getBasicAuth();
        this.client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool())
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(20000, TimeUnit.MILLISECONDS)
                .build();
    }

    public JsonNode getJsonNode(final String url) throws IOException {
        final OkHttpClient newClient = client.newBuilder().build();
        final Request request = new Request.Builder()
                .header("Authorization", this.authentication)
                .header("Cache-Control", "no-store")
                .url(url)
                .build();
        final Response response = newClient.newCall(request).execute();
        try (ResponseBody responseBody = response.body()){

            if (!response.isSuccessful()) {
                final StringBuilder sb = new StringBuilder();
                Headers requestHeaders = request.headers();
                Headers responseHeaders = response.headers();

                sb.append("{");
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
                sb.append("}");
                sb.append("}");

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
                sb.append("}");
                sb.append("}");
                log.debug("Encountered connection error: " + sb);
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

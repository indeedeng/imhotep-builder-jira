package com.indeed.jiraactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.jiraactions.JiraActionsIndexBuilderConfig;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Map;

public abstract class ApiCaller {
    protected final JiraActionsIndexBuilderConfig config;

    private static final Logger log = Logger.getLogger(ApiCaller.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String authentication;

    public ApiCaller(final JiraActionsIndexBuilderConfig config) {
        this.config = config;
        this.authentication = getBasicAuth();
    }

    protected JsonNode getJsonNode(final String url) throws IOException {
        HttpsURLConnection urlConnection = null;
        Map<String, List<String>> headers = null;
        BufferedReader br = null;
        String apiResults = null;
        try {
            urlConnection = getURLConnection(url);
            headers = urlConnection.getRequestProperties();
            final InputStream in = urlConnection.getInputStream();
            br = new BufferedReader(new InputStreamReader(in));
            apiResults = br.readLine();
            return objectMapper.readTree(apiResults);
        } catch (final IOException e) {
            final StringBuilder sb = new StringBuilder();

            sb.append("{");
            sb.append("\"Request\": {");

            sb.append("\"URL\": \"").append(url).append("\",");
            if (headers != null) {
                sb.append("\"Headers\": {");
                for (final Map.Entry<String, List<String>> header : headers.entrySet()) {
                    final String key = header.getKey();
                    final String value;
                    if ("Authorization".equals(key)) {
                        value = "<Omitted>";
                    } else {
                        value = String.join(",", header.getValue());
                    }
                    sb.append("\"").append(key).append("\": \"").append(value).append("\",");
                }
            }
            sb.append("}");

            sb.append("}");

            if (urlConnection != null) {
                sb.append(", \"Response\": {");
                for (final Map.Entry<String, List<String>> header : urlConnection.getHeaderFields().entrySet()) {
                    final String key = header.getKey();
                    final String value;
                    if ("Set-Cookie".equals(key)) {
                        value = "<Omitted>";
                    } else {
                        value = String.join(",", header.getValue());
                    }
                    sb.append("\"").append(key).append("\": \"").append(value).append("\",");
                }
                sb.append("\"Code\": ").append(urlConnection.getResponseCode()).append(",");
                sb.append("\"Message\": \"").append(urlConnection.getResponseMessage()).append("\",");
            }
            if (apiResults != null) {
                sb.append("\"Body\": \"").append(apiResults).append("\"");
            }
            if (urlConnection != null) {
                final InputStream error = urlConnection.getErrorStream();
                final BufferedReader errorBr = new BufferedReader(new InputStreamReader(error));
                sb.append("\"Error Body\": \"");
                String line = errorBr.readLine();
                while (line != null) {
                    sb.append(line).append(System.lineSeparator());
                    line = errorBr.readLine();
                }
                errorBr.close();
                sb.append("\"");
            }

            sb.append("}");
            sb.append("}");
            log.error("Encountered connection error: " + sb);
            throw e;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (final IOException ignored) {
                }
            }
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

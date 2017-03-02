package com.indeed.skeleton.index.builder.jiraaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.common.util.StringUtils;
import com.indeed.util.logging.Loggers;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * @author soono
 */
public class IssuesAPICaller {
    private static final Logger log = Logger.getLogger(IssuesAPICaller.class);
    private final JiraActionIndexBuilderConfig config;
    //
    // For Pagination
    //

    private final int numPerPage; // Max number of issues per page
    private int page = 0; // Current Page
    private int numTotal = -1; // Total number of issues remaining

    public IssuesAPICaller(final JiraActionIndexBuilderConfig config) {
        this.config = config;
        this.numPerPage = config.getJiraBatchSize();
    }

    public JsonNode getIssuesNode() throws IOException {
        final JsonNode apiRes = getJsonNode(getIssuesURL());
        setNextPage();
        return apiRes.get("issues");
    }

    //
    // Call API with URL and parse response to JSON node.
    //

    private JsonNode getJsonNode(final String url) throws IOException {
        final HttpsURLConnection urlConnection = getURLConnection(url);
        final InputStream in = urlConnection.getInputStream();
        final BufferedReader br = new BufferedReader(new InputStreamReader(in));
        final String apiRes = br.readLine();
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(apiRes);
    }

    //
    // For Pagination
    //

    public int setNumTotal() throws IOException {
        final JsonNode apiRes = getJsonNode(getBasicInfoURL());
        final JsonNode totalNode = apiRes.path("total");
        this.numTotal = totalNode.intValue();
        return numTotal;
    }

    public boolean currentPageExist() {
        return (page * numPerPage) < numTotal;
    }

    private void setNextPage() {
            page +=1;
    }

    private int getStartAt() {
        // startAt starts from 0
        return page * numPerPage;
    }


    //
    // For Getting URL Connection
    //

    private HttpsURLConnection getURLConnection(final String urlString) throws IOException {
        final URL url = new URL(urlString);
        final HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Authorization", getBasicAuth());
        return urlConnection;
    }

    private String getBasicAuth() {
        final String userPass = config.getJiraUsernameIndexer() + ":" + config.getJiraPasswordIndexer();
        final String basicAuth = "Basic " + new String(new Base64().encode(userPass.getBytes()));
        return basicAuth;
    }

    private String getIssuesURL() throws UnsupportedEncodingException {
        final String url = new StringBuilder(config.getJiraBaseURL() + "?")
                .append(getJQLParam())
                .append("&")
                .append(getFieldsParam())
                .append("&")
                .append(getExpandParam())
                .append("&")
                .append(getStartAtParam())
                .append("&")
                .append(getMaxResults())
                .toString();

        final int start = getStartAt();
        Loggers.debug(log, "Trying URL: %s", url);
        Loggers.info(log, "%f%% complete, %d/%d", (float)start*100/numTotal, start, numTotal);

        return url;
    }

    private String getBasicInfoURL() throws UnsupportedEncodingException {
        final StringBuilder url = new StringBuilder(config.getJiraBaseURL() + "?")
                .append(getJQLParam())
                .append("&maxResults=0");
        return url.toString();
    }

    private String getJQLParam() throws UnsupportedEncodingException {
        final StringBuilder query = new StringBuilder();
        if(config.isBackfill()) {
            query.append("(");
        }
        query.append("(").append("updatedDate>=").append(config.getStartDate())
                .append(" AND updatedDate<").append(config.getEndDate())
                .append(")");
        if(config.isBackfill()) {
            query.append(" AND ")
                    .append("(")
                    .append("createdDate>=").append(config.getStartDate())
                    .append(" AND createdDate<").append(config.getEndDate())
                    .append(")")
                    .append(")");
        }

        if(!StringUtils.isEmpty(config.getJiraProject())) {
            query.append(" AND project IN (").append(config.getJiraProject()).append(")");
        }

        return "jql=" + URLEncoder.encode(query.toString(), "UTF-8");
    }

    private String getFieldsParam() {
        return String.format("fields=%s", config.getJiraFields());
    }

    private String getExpandParam() {
        return String.format("expand=%s", config.getJiraExpand());
    }

    private String getStartAtParam() {
        return String.format("startAt=%d", getStartAt());
    }

    private String getMaxResults() {
        return String.format("maxResults=%d", numPerPage);
    }
}
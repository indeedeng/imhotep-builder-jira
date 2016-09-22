package com.indeed.skeleton.index.builder.jiraaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by soono on 8/30/16.
 */
public class IssuesAPICaller {

    private ConfigReader configReader = new PropertiesConfigReader();

    //
    // For Pagination
    //

    private final int numPerPage = 5; // Max number of issues per page
    private int page = 0; // Current Page
    private int numTotal = -1; // Max number of issues per page

    public IssuesAPICaller() throws IOException {
        setNumTotal();
    }

    public JsonNode getIssuesNode() throws IOException {
        JsonNode apiRes = getJsonNode(getIssuesURL());
        setNextPage();
        return apiRes.get("issues");
    }

    //
    // Call API with URL and parse response to JSON node.
    //

    private JsonNode getJsonNode(String url) throws IOException {
        final HttpsURLConnection urlConnection = getURLConnection(url);
        InputStream in = urlConnection.getInputStream();
        final BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String apiRes = br.readLine();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(apiRes);
    }

    //
    // For Pagination
    //

    private void setNumTotal() throws IOException {
        JsonNode apiRes = getJsonNode(getBasicInfoURL());
        JsonNode totalNode = apiRes.path("total");
        this.numTotal = totalNode.intValue();
    }

    public boolean currentPageExist() {
        return (page * numPerPage) < numTotal;
    }

    private int setNextPage() {
            return ++page;
    }

    private int getStartAt() {
        // startAt starts from 0
        return page * numPerPage;
    }


    //
    // For Getting URL Connection
    //

    private HttpsURLConnection getURLConnection(String urlString) throws IOException {
        System.out.println(urlString);
        URL url = new URL(urlString);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Authorization", getBasicAuth());
        return urlConnection;
    }

    private String getBasicAuth() {
        String userPass = configReader.username() + ":" + configReader.password();
        String basicAuth = "Basic " + new String(new Base64().encode(userPass.getBytes()));
        return basicAuth;
    }

    private String getIssuesURL() {
        final StringBuilder url = new StringBuilder(configReader.jiraBaseURL() + "?");
        url.append(getJQLParam());
        url.append("&");
        url.append(getFieldsParam());
        url.append("&");
        url.append(getExpandParam());
        url.append("&");
        url.append(getStartAtParam());
        url.append("&");
        url.append(getMaxResults());
        return url.toString();
    }

    private String getBasicInfoURL() {
        final StringBuilder url = new StringBuilder(configReader.jiraBaseURL() + "?");
        url.append(getJQLParam());
        url.append("&maxResults=0");
        return url.toString();
    }

    private String getJQLParam() {
        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        final String yesterday = dateFormat.format(cal.getTime());

        return String.format("jql=updatedDate>=%s", yesterday);
    }

    private String getFieldsParam() {
        return String.format("fields=%s", configReader.jiraFields());
    }

    private String getExpandParam() {
        return String.format("expand=%s", configReader.jiraExpand());
    }

    private String getStartAtParam() {
        return String.format("startAt=%d", getStartAt());
    }

    private String getMaxResults() {
        return String.format("maxResults=%d", numPerPage);
    }
}
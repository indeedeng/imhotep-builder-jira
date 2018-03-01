package com.indeed.jiraactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.indeed.jiraactions.JiraActionsIndexBuilderConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author soono
 */
public class IssuesAPICaller extends ApiCaller {
    private static final Logger log = Logger.getLogger(IssuesAPICaller.class);
    private static final String API_PATH = "/rest/api/2/search";

    private final String urlBase;

    // For Pagination
    private final int maxPerPage; // Max number of issues per page
    private int batchSize;
    private int start = 0; // Current Page
    private int numTotal = -1; // Total number of issues remaining

    private int backoff = 10_000;

    public IssuesAPICaller(final JiraActionsIndexBuilderConfig config) throws UnsupportedEncodingException {
        super(config);
        this.maxPerPage = config.getJiraBatchSize()*2;
        this.batchSize = config.getJiraBatchSize();

        this.urlBase = getIssuesUrlBase();
    }

    public JsonNode getIssuesNodeWithBackoff() throws InterruptedException {
        int tries = 0;
        while (true) {
            final long start = System.currentTimeMillis();
            try {
                tries++;
                final JsonNode node = getIssuesNode();
                backoff = Math.max(backoff / 2, 10_000);
                batchSize = Math.min(batchSize + 2, maxPerPage);
                return node;
            } catch (final IOException e) {
                final long end = System.currentTimeMillis();
                log.error(String.format("On try %d/5, caught IOException getting %d issues, after %d milliseconds.",
                        tries, batchSize, end - start));

                if(tries >= 5) {
                    log.error("Tried too many times to get issues and failed, aborting.", e);
                    throw new RuntimeException(e);
                }

                batchSize = Math.max(batchSize - (int)(batchSize*(float)0.9), 1);
                log.warn("Caught exception when trying to get issues, backing off for " + backoff + " milliseconds" +
                        " and trying again with batchSize = " + batchSize, e);
                Thread.sleep(backoff);
                backoff *= 2;
            }
        }
    }

    private JsonNode getIssuesNode() throws IOException {
        final JsonNode apiRes = getJsonNode(getIssuesURL());
        setNextPage();
        this.numTotal = apiRes.get("total").intValue();
        return apiRes.get("issues");
    }

    public int setNumTotal() throws IOException {
        final JsonNode apiRes = getJsonNode(getBasicInfoURL());
        final JsonNode totalNode = apiRes.path("total");
        final int total = totalNode.intValue();
        this.numTotal = total;
        return numTotal;
    }

    public boolean currentPageExist() {
        return start < numTotal;
    }

    private void setNextPage() {
        start += batchSize;
    }

    public void reset() {
        start = 0;
    }

    private String getIssuesUrlBase() throws UnsupportedEncodingException {
        return config.getJiraBaseURL() + API_PATH + "?" +
                getJQLParam() +
                "&" +
                getFieldsParam() +
                "&" +
                getExpandParam();
    }

    private String getIssuesURL() {
        final String url = urlBase
                + "&" + getMaxResults()
                + "&" + getStartAtParam();

        if(log.isDebugEnabled()) {
            log.debug(String.format("Trying URL: %s", url));
        }
        log.info(String.format("%f%% complete, %d/%d", (float)start*100/numTotal, start, numTotal));

        return url;
    }

    private String getBasicInfoURL() throws UnsupportedEncodingException {
        final String url = config.getJiraBaseURL() + API_PATH + "?" +
                getJQLParam() +
                "&maxResults=0";
        return url;
    }

    private String getJQLParam() throws UnsupportedEncodingException {
        final StringBuilder query = new StringBuilder();

        /* We want to get everything that existed between our start and end dates, and we'll filter out individual
         * actions elsewhere. So only select issues that were updated since we started (i.e., exclude things that
         * have not been updated since our start) and only issues that were created before our end (i.e., exclude things
         * that were created after we started).
         */
        query.append("updatedDate>=").append(config.getStartDate())
                .append(" AND createdDate<").append(config.getEndDate());

        if(!StringUtils.isEmpty(config.getJiraProject())) {
            query.append(" AND project IN (").append(config.getJiraProject()).append(")");
        }

        if(!StringUtils.isEmpty(config.getExcludedJiraProject())) {
            query.append(" AND project NOT IN (").append(config.getExcludedJiraProject()).append(")");
        }

        query.append(" ORDER BY updatedDate DESC, issuekey DESC"); // seems like updatedDate isn't quite repeatable

        return "jql=" + URLEncoder.encode(query.toString(), "UTF-8");
    }

    private String getFieldsParam() {
        if(config.getCustomFields() == null || config.getCustomFields().length == 0) {
            return String.format("fields=%s", config.getJiraFields());
        } else {
            return "fields=" +
                    String.join(",", config.getJiraFields(),
                    String.join(",", Arrays.stream(config.getCustomFields()).flatMap(x -> Arrays.stream(x.getCustomFieldId())).collect(Collectors.toList())));
        }
    }

    private String getExpandParam() {
        return String.format("expand=%s", config.getJiraExpand());
    }

    private String getStartAtParam() {
        return String.format("startAt=%d", start);
    }

    private String getMaxResults() {
        return String.format("maxResults=%d", batchSize);
    }
}
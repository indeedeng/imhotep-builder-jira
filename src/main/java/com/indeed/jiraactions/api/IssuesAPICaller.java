package com.indeed.jiraactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.indeed.jiraactions.JiraActionsIndexBuilderConfig;
import com.indeed.jiraactions.JiraActionsUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author soono
 */
public class IssuesAPICaller {
    private static final Logger log = LoggerFactory.getLogger(IssuesAPICaller.class);
    private static final String API_PATH = "/rest/api/2/search";

    private final String urlBase;
    private final ApiCaller apiCaller;
    private final JiraActionsIndexBuilderConfig config;
    private final boolean buildJiraIssuesApi;

    // For Pagination
    private final int maxPerPage; // Max number of issues per page
    private int batchSize;
    private int start = 0; // Current Page
    private int numTotal = -1; // Total number of issues remaining

    private int backoff = 10_000;

    public IssuesAPICaller(final JiraActionsIndexBuilderConfig config, final ApiCaller apiCaller, final boolean buildJiraIssuesApi) throws UnsupportedEncodingException {
        this.config = config;
        this.apiCaller = apiCaller;
        this.buildJiraIssuesApi = buildJiraIssuesApi;

        maxPerPage = config.getJiraBatchSize()*2;
        batchSize = config.getJiraBatchSize();

        urlBase = getIssuesUrlBase();
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
                log.error("On try {}/5, caught IOException getting {} issues, after {} milliseconds.",
                        tries, batchSize, end - start);

                if(tries >= 5) {
                    log.error("Tried too many times to get issues and failed, aborting.", e);
                    throw new RuntimeException(e);
                }

                batchSize = Math.max(batchSize - (int)(batchSize*(float)0.9), 1);
                log.warn("Caught exception when trying to get issues, backing off for {} milliseconds and trying again with batchSize = {}", backoff, batchSize, e);
                Thread.sleep(backoff);
                backoff *= 2;
            }
        }
    }

    private JsonNode getIssuesNode() throws IOException {
        final JsonNode apiRes = apiCaller.getJsonNode(getIssuesURL());
        setNextPage();
        this.numTotal = apiRes.get("total").intValue();
        return apiRes.get("issues");
    }

    public int setNumTotal() throws IOException {
        final JsonNode apiRes = apiCaller.getJsonNode(getBasicInfoURL());
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
            log.debug("Trying URL: {}", url);
        }
        log.info("{}% complete, {}/{}", (float)start*100/numTotal, start, numTotal);

        return url;
    }

    private String getBasicInfoURL() throws UnsupportedEncodingException {
        final String url = config.getJiraBaseURL() + API_PATH + "?" +
                getJQLParam() +
                "&maxResults=0";
        return url;
    }

    protected static final DateTimeZone JIRA_TIME_ZONE = DateTimeZone.forID("America/Chicago");
    private static final DateTimeFormatter JIRA_TIME_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
    /**
     * Imhotep builders always runs in UTC-6, regardless of DST. However, Jira observes daylight savings time.
     * So when we're in DST and run for "2018-04-01" to "2018-04-02", we need to tell Jira we actually mean
     * "2018-04-01T01:00:00" to "2018-04-02T01:00:00"
     */
    @VisibleForTesting
    protected static String getDateStringInJiraTime(final String dateString) {
        final DateTime date = JiraActionsUtil.parseDateTime(dateString);
        final DateTime adjusted = date.toDateTime(JIRA_TIME_ZONE);
        return JIRA_TIME_FORMAT.print(adjusted);
    }

    private String getJQLParam() throws UnsupportedEncodingException {
        final StringBuilder query = new StringBuilder();

        /* We want to get everything that existed between our start and end dates, and we'll filter out individual
         * actions elsewhere. So only select issues that were updated since we started (i.e., exclude things that
         * have not been updated since our start) and only issues that were created before our end (i.e., exclude things
         * that were created after we started).
         */

        final String start = buildJiraIssuesApi ? getDateStringInJiraTime(JiraActionsUtil.parseDateTime(config.getStartDate()).minusMonths(config.getSnapshotLookbackMonths()).toString()) : getDateStringInJiraTime(config.getStartDate());
        final String end = getDateStringInJiraTime(config.getEndDate());
        query.append("updatedDate>=\"").append(start)
                .append("\" AND createdDate<\"").append(end).append("\"");

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

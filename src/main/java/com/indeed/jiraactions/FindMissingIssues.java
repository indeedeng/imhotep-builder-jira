package com.indeed.jiraactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.jiraactions.api.ApiCaller;
import com.indeed.jiraactions.api.IssueAPIParser;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.response.issue.Issue;
import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Base64;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 *
 */
public class FindMissingIssues extends ApiCaller {
    public FindMissingIssues(final JiraActionsIndexBuilderConfig config) {
        super(config);
    }

    public static void main(final String[] args) throws IOException, EncoderException {
        final File file = new File("/home/kbinswanger/Downloads/issueswithreviews.txt");
        final Scanner scanner = new Scanner(file);
        final List<String> issues = new ArrayList<>();
        while(scanner.hasNextLine()) {
            issues.add(scanner.nextLine());
        }
        final String BASE_URL = "https://bugs.indeed.com/rest/api/2/search?jql=issuekey=%s&fields=assignee,comment,creator,issuetype,project,status,resolution,summary,reporter,created,category,fixVersions,duedate,components,labels,priority,customfield_11790,customfield_11791,customfield_12994,customfield_15290,customfield_12492,customfield_12495,customfield_12491,customfield_12090,customfield_17591,customfield_11490,customfield_17490,customfield_17090,customfield_10002,customfield_10003,customfield_11590,customfield_17991,customfield_17992,customfield_17998,customfield_18002,customfield_18007,customfield_17993,customfield_17994,customfield_17995,customfield_17996,customfield_17997,customfield_18990,customfield_17999,customfield_18000,customfield_18001,customfield_18003,customfield_18004,customfield_18005,customfield_18006,customfield_18008,customfield_18009,customfield_19691,customfield_18790,customfield_18791,customfield_18890,customfield_19690,customfield_18891,customfield_18892,customfield_19693,customfield_18894,customfield_18895,customfield_18896,customfield_18897,customfield_18898,customfield_18899,customfield_18900,customfield_19692,customfield_18901,customfield_14026,customfield_19022,customfield_16591,customfield_20290,customfield_14090,customfield_19013&expand=changelog&maxResults=25&startAt=0";
        final String BASE_UPSOURCE_URL = "https://upsource.corp.indeed.com/~rpc/getReviewDetails?params={\"projectId\":\"%s\",\"reviewId\":\"%s\"}";
        final ObjectMapper objectMapper = new ObjectMapper();

        final JiraActionsIndexBuilderConfig config = new JiraActionsIndexBuilderConfig() {
            @Override
            public String getJiraUsername() {
                return "";
            }

            @Override
            public String getJiraPassword() {
                return "";
            }

            @Override
            public String getJiraBaseURL() {
                throw new UnsupportedOperationException("You need to implement this");
            }

            @Override
            public String getJiraFields() {
                throw new UnsupportedOperationException("You need to implement this");
            }

            @Override
            public String getJiraExpand() {
                throw new UnsupportedOperationException("You need to implement this");
            }

            @Override
            public String getJiraProject() {
                throw new UnsupportedOperationException("You need to implement this");
            }

            @Override
            public String getExcludedJiraProject() {
                throw new UnsupportedOperationException("You need to implement this");
            }

            @Override
            public String getIuploadURL() {
                throw new UnsupportedOperationException("You need to implement this");
            }

            @Override
            public String getIuploadUsername() {
                throw new UnsupportedOperationException("You need to implement this");
            }

            @Override
            public String getIuploadPassword() {
                throw new UnsupportedOperationException("You need to implement this");
            }

            @Override
            public String getStartDate() {
                throw new UnsupportedOperationException("You need to implement this");
            }

            @Override
            public String getEndDate() {
                throw new UnsupportedOperationException("You need to implement this");
            }

            @Override
            public int getJiraBatchSize() {
                throw new UnsupportedOperationException("You need to implement this");
            }

            @Override
            public String getIndexName() {
                throw new UnsupportedOperationException("You need to implement this");
            }

            @Override
            public CustomFieldDefinition[] getCustomFields() {
                throw new UnsupportedOperationException("You need to implement this");
            }
        };

        final String authentication =  "Basic " + new String(
                new Base64()
                        .encode((config.getJiraUsername() + ":" + config.getJiraPassword()).getBytes()));

        final FindMissingIssues api = new FindMissingIssues(config);

        final int total = issues.size();
        int j = 0;
        for(final String issuekey : issues) {
            if(j % 10 == 0) {
                System.out.println(String.format("%f%% done", j/(double)total*100));
            }
            j++;
            final String url = String.format(BASE_URL, issuekey);
            final JsonNode issuesNode = api.getJsonNode(url).get("issues");
            for (final JsonNode issueNode : issuesNode) {
                final Issue issue = IssueAPIParser.getObject(issueNode);
                final List<Item[]> items = Arrays.stream(issue.changelog.histories)
                        .filter(h -> "Upsource Service".equals(h.author.getDisplayName()))
                        .map(h -> h.items).collect(Collectors.toList());
                for (final Item[] a : items) {
                    for (final Item i : a) {
                        final String review = i.toString.substring(34, i.toString.length() - 12);
                        final int reviewNumIndex = review.lastIndexOf("-CR-");
                        final String repo = review.substring(0, reviewNumIndex);


                        final String reviewUrl = String.format(BASE_UPSOURCE_URL,
                            repo.toLowerCase(), review);
                        final HttpsURLConnection connection = getURLConnection(reviewUrl, authentication);
                        final int responseCode = connection.getResponseCode();
                        if(responseCode != 200) {
                            System.err.println(String.format("Issue %s has missing review %s (response code: %d), verify at url: '%s'",
                                    issue.key, review, responseCode, String.format("https://upsource.corp.indeed.com/%s/review/%s", repo.toLowerCase(), review)));
                        } else {
                            final String response = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
                            final String title = objectMapper.readTree(response).get("result").get("title").asText();
                            if(!title.contains(issuekey)) {
                                System.err.println(String.format("Issue %s potentially has overwritten review %s. Title is '%s', verify at url: '%s'",
                                        issue.key, review, title, String.format("https://upsource.corp.indeed.com/%s/review/%s", repo.toLowerCase(), review)));
                            }
                        }
                    }
                }
            }
        }
    }

    private static HttpsURLConnection getURLConnection(final String urlString, final String authentication) throws IOException {
        final URL url = new URL(urlString);
        final HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Authorization", authentication);
        return urlConnection;
    }
}

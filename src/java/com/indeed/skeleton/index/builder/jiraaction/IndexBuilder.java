package com.indeed.skeleton.index.builder.jiraaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;

import java.util.List;

/**
 * Created by soono on 8/30/16.
 */
public class IndexBuilder {

    public static void main (final String[] args) throws Exception {

        JiraActionsTool.main(args);

        final long start_total = System.currentTimeMillis();
        final long end_total;

        final IssuesAPICaller issuesAPICaller = new IssuesAPICaller();

        List<Action> actions = null;

        long start, end;

        while(issuesAPICaller.currentPageExist()){
            // Get issues from API.
            start = System.currentTimeMillis();
            final JsonNode issuesNode = issuesAPICaller.getIssuesNode();
            end = System.currentTimeMillis();
            System.out.println((end - start) + "ms for an API call.");

            start = System.currentTimeMillis();
            for (final JsonNode issueNode : issuesNode) {
                // Parse Each Issue API response to Object.
                final Issue issue = IssueAPIParser.getObject(issueNode);

                // Build Action objects from parsed API response Object.
                final ActionsBuilder actionsBuilder = new ActionsBuilder(issue);

                // Set built actions to actions list.
                if (actions == null) {
                    actions = actionsBuilder.buildActions();
                } else {
                    actions.addAll(actionsBuilder.buildActions());
                }
            }
            end = System.currentTimeMillis();
            System.out.println((end - start) + "ms to get actions from a set of issues.");

            Thread.sleep(10000);
        }

        start = System.currentTimeMillis();
        // Create and Upload a TSV file.
        TsvFileWriter.createTSVFile(actions);
        end = System.currentTimeMillis();
        System.out.println((end - start) + "ms to create and upload TSV.");

        end_total = System.currentTimeMillis();
        System.out.println((end_total - start_total) + "ms for the whole process.");
    }
}

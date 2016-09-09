package com.indeed.skeleton.index.builder.jiraaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.indeed.flamdex.writer.FlamdexDocument;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;
import org.apache.avro.generic.GenericData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by soono on 8/30/16.
 */
public class IndexBuilder {

    public static void main (String[] args) throws Exception {

        IssuesAPICaller issuesAPICaller = new IssuesAPICaller();

        List<Action> actions = null;

        while(issuesAPICaller.currentPageExist()){
            // Get issues from API.
            JsonNode issuesNode = issuesAPICaller.getIssuesNode();
            for (final JsonNode issueNode : issuesNode) {
                // Parse Each Issue API response to Object.
                Issue issue = IssueAPIParser.getObject(issueNode);

                // Build Action objects from parsed API response Object.
                ActionsBuilder actionsBuilder = new ActionsBuilder(issue);

                // Set built actions to actions list.
                if (actions == null) {
                    actions = actionsBuilder.buildActions();
                } else {
                    actions.addAll(actionsBuilder.buildActions());
                }
            }
        }

        // Create a TSV file.
        TsvFileWriter.createTSVFile(actions);
    }
}

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
        while(issuesAPICaller.currentPageExist()){
            // Get issues from API.
            JsonNode issuesNode = issuesAPICaller.getIssuesNode();
            for (final JsonNode issueNode : issuesNode) {
                // Parse Each Issue API response to Object.
                Issue issue = IssueAPIParser.getObject(issueNode);

                // Build Action object from parsed API response Object.
                ActionsBuilder actionsBuilder = new ActionsBuilder(issue);
                List<Action> actions = actionsBuilder.buildActions();

                // Parse and Save Document.
                for ( Action action : actions) {
                    FlamdexDocument doc = DocumentParser.parse(action);
                    // TODO: save document.
                }
            }
        }

    }

}

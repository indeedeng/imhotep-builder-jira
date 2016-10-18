package com.indeed.skeleton.index.builder.jiraaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;
import com.indeed.util.logging.Loggers;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * @author oono
 */
public class JiraActionIndexBuilder {
    private static final Logger log = Logger.getLogger(JiraActionIndexBuilder.class);

    private final JiraActionIndexBuilderConfig config;

    public JiraActionIndexBuilder(final JiraActionIndexBuilderConfig config) {
        this.config = config;
    }

    public void run() {
        try {
            final long start_total = System.currentTimeMillis();
            final long end_total;

            final IssuesAPICaller issuesAPICaller = new IssuesAPICaller(config);
            issuesAPICaller.setNumTotal();

            List<Action> actions = null;

            long start, end;

            while (issuesAPICaller.currentPageExist()) {
                // Get issues from API.
                start = System.currentTimeMillis();
                final JsonNode issuesNode = issuesAPICaller.getIssuesNode();
                end = System.currentTimeMillis();
                Loggers.info(log, "%d ms for an API call.", end - start);

                start = System.currentTimeMillis();
                for (final JsonNode issueNode : issuesNode) {
                    // Parse Each Issue API response to Object.
                    final Issue issue = IssueAPIParser.getObject(issueNode);
                    if(issue == null) {
                        continue;
                    }

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
                Loggers.info(log, "%d to get actions from a set of issues.", end - start);

                Thread.sleep(10000);
            }


            start = System.currentTimeMillis();
            // Create and Upload a TSV file.
            final TsvFileWriter writer = new TsvFileWriter(config);
            writer.createTSVFile(actions);
            end = System.currentTimeMillis();
            Loggers.info(log, "%d ms to create and upload TSV.", end - start);

            end_total = System.currentTimeMillis();

            Loggers.info(log, "%d ms for the whole process.", end_total - start_total);
        } catch (final Exception e) {
            log.error("Threw an exception trying to run the index builder", e);
        }
    }
}

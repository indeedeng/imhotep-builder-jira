package com.indeed.skeleton.index.builder.jiraaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;
import com.indeed.util.logging.Loggers;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.util.ArrayList;
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

    public void run() throws Exception {
        try {
            final long start_total = System.currentTimeMillis();
            final long end_total;

            final IssuesAPICaller issuesAPICaller = new IssuesAPICaller(config);
            {
                final long start = System.currentTimeMillis();
                final int total = issuesAPICaller.setNumTotal();
                final long end = System.currentTimeMillis();
                Loggers.info(log, "%d ms, found %d total issues.", end - start, total);
            }

            final List<Action> actions = new ArrayList<>();

            long start, end;

            final DateTime startDate = JiraActionUtil.parseDateTime(config.getStartDate());

            final DateTime endDate = JiraActionUtil.parseDateTime(config.getEndDate());

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

                    try {
                        // Build Action objects from parsed API response Object.
                        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);

                        // Set built actions to actions list.
                        actions.addAll(actionsBuilder.buildActions());
                    } catch(final Exception e) {
                        Loggers.error(log, "Error parsing comments for issue %s.", e, issue.key);
                        continue;
                    }
                }
                end = System.currentTimeMillis();
                Loggers.info(log, "%d ms to get actions from a set of issues.", end - start);

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
            throw e;
        }
    }
}

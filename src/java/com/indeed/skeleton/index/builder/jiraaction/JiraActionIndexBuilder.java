package com.indeed.skeleton.index.builder.jiraaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.Issue;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

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
                log.info(String.format("%d ms, found %d total issues.", end - start, total));
            }

            long apiTime =0;
            long processTime = 0;
            long fileTime = 0;

            long start, end;

            final DateTime startDate = JiraActionUtil.parseDateTime(config.getStartDate());
            final DateTime endDate = JiraActionUtil.parseDateTime(config.getEndDate());

            final TsvFileWriter writer = new TsvFileWriter(config);
            start = System.currentTimeMillis();
            writer.createFileAndWriteHeaders();
            end = System.currentTimeMillis();
            fileTime += end - start;

            while (issuesAPICaller.currentPageExist()) {
                // Get issues from API.
                start = System.currentTimeMillis();
                final JsonNode issuesNode = issuesAPICaller.getIssuesNode();
                end = System.currentTimeMillis();
                apiTime += end-start;
                log.info(String.format("%d ms for an API call.", end - start));

                start = System.currentTimeMillis();
                for (final JsonNode issueNode : issuesNode) {
                    // Parse Each Issue API response to Object.
                    long process_start = System.currentTimeMillis();
                    final Issue issue = IssueAPIParser.getObject(issueNode);
                    long process_end = System.currentTimeMillis();
                    processTime += process_end - process_start;
                    if(issue == null) {
                        continue;
                    }

                    try {
                        // Build Action objects from parsed API response Object.
                        process_start = System.currentTimeMillis();
                        final ActionsBuilder actionsBuilder = new ActionsBuilder(issue, startDate, endDate);
                        final List<Action> actions = actionsBuilder.buildActions();
                        process_end = System.currentTimeMillis();
                        processTime += process_end - process_start;

                        // Set built actions to actions list.
                        final long file_start = System.currentTimeMillis();
                        writer.writeActions(actions);
                        final long file_end = System.currentTimeMillis();
                        fileTime += file_end - file_start;
                    } catch(final Exception e) {
                        log.error(String.format("Error parsing actions for issue %s.", issue.key), e);
                    }
                }
                end = System.currentTimeMillis();
                log.info(String.format("%d ms to get actions from a set of issues.", end - start));

                Thread.sleep(10000);
            }

            start = System.currentTimeMillis();
            // Create and Upload a TSV file.
            writer.uploadTsvFile();
            end = System.currentTimeMillis();
            log.info(String.format("%d ms to create and upload TSV.", end - start));
            fileTime += end - start;

            end_total = System.currentTimeMillis();

            log.info(String.format("%d ms for the whole process.", end_total - start_total));
            log.info(String.format("apiTime: %dms, processTime: %dms, fileTime: %dms", apiTime, processTime, fileTime));
        } catch (final Exception e) {
            log.error("Threw an exception trying to run the index builder", e);
            throw e;
        }
    }
}

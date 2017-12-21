package com.indeed.jiraactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.indeed.jiraactions.api.ApiUserLookupService;
import com.indeed.jiraactions.api.IssueAPIParser;
import com.indeed.jiraactions.api.IssuesAPICaller;
import com.indeed.jiraactions.api.customfields.CustomFieldApiParser;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.response.issue.Issue;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author oono
 */
public class JiraActionsIndexBuilder {
    private static final Logger log = Logger.getLogger(JiraActionsIndexBuilder.class);

    private final JiraActionsIndexBuilderConfig config;

    public JiraActionsIndexBuilder(final JiraActionsIndexBuilderConfig config) {
        this.config = config;
    }

    public void run() throws Exception {
        try {
            final long start_total = System.currentTimeMillis();
            final long end_total;

            final ApiUserLookupService userLookupService = new ApiUserLookupService(config);
            final CustomFieldApiParser customFieldApiParser = new CustomFieldApiParser(userLookupService);
            final ActionFactory actionFactory = new ActionFactory(userLookupService, customFieldApiParser, config);

            final IssuesAPICaller issuesAPICaller = new IssuesAPICaller(config);
            initializeIssuesApiCaller(issuesAPICaller);

            if(!issuesAPICaller.currentPageExist()) {
                log.warn("No issues found for this time range.");
                return;
            }

            long apiTime = 0;
            long processTime = 0;
            long fileTime = 0;

            long start, end;

            final DateTime startDate = JiraActionsUtil.parseDateTime(config.getStartDate());
            final DateTime endDate = JiraActionsUtil.parseDateTime(config.getEndDate());

            final TsvFileWriter writer = new TsvFileWriter(config);
            start = System.currentTimeMillis();
            writer.createFileAndWriteHeaders();
            end = System.currentTimeMillis();
            fileTime += end - start;

            final Map<String, DateTime> seenIssues = new LinkedHashMap<>();
            /*
                 * This is kind of complicated, and there's a write-up in JAI-5. But the basic idea is that we want to
                 * process the entire run first. Then, we start over. But we only want to process until we get to the
                 * point where everything is the same as we've already seen. When that happens, we stop.
                 *
                 * So imagine we see have 26 issues, A-Z. Issues A-Y fall into our time ranger, but Z is too old.
                 * We process A, B, C, D, E.
                 * We process F, G, H, I, J. At this point, issue Z is modified (and goes to the beginning of our search).
                 * We process J, K, L, M, N. At this point, issue E is modified (and goes to the beginning of our search).
                 * ...
                 * Eventually we get to the end and process Y. At this point, issue A is modified (and goes to the beginning of our search).
                 *
                 * Then we want to reprocess A, E, Z, and then stop.
                 *
                 * Every time we process an issue, we remove it from our seenIssues map and re-add it (putting it at the
                 * end). Consequently, we know we can stop when we see the first issue in our seenIssues map *and* it
                 * has no new actions.
                 *
                 * There are three scenarios:
                 * 1) We are just processing all the issues. Continue through to the end.
                 * 2) We are doing a second pass, but we had to read through a bunch of issues until we get to the old beginning.
                 *    Then we should bail out and start at the beginning again to just pick up what's new during this pass.
                 * 3) We find something we've already seen at the very beginning of our list. We're done.
                 */
            boolean reFoundTheBeginning = false;
            boolean firstIssue = true;
            boolean firstPass = true;
            final ImmutableSet.Builder<CustomFieldDefinition> customFieldsSeen = ImmutableSet.builder();
            while (!reFoundTheBeginning || !firstIssue) {
                reFoundTheBeginning = false;
                firstIssue = true;
                while (issuesAPICaller.currentPageExist()) {
                    start = System.currentTimeMillis();
                    final JsonNode issuesNode = issuesAPICaller.getIssuesNodeWithBackoff();
                    end = System.currentTimeMillis();
                    apiTime += end - start;
                    log.trace(String.format("%d ms for an API call.", end - start));

                    start = System.currentTimeMillis();
                    for (final JsonNode issueNode : issuesNode) {
                        // Parse Each Issue API response to Object.
                        long process_start = System.currentTimeMillis();
                        final Issue issue = IssueAPIParser.getObject(issueNode);
                        long process_end = System.currentTimeMillis();
                        processTime += process_end - process_start;
                        if (issue == null) {
                            log.error("null issue after parsing: " + issueNode.toString());
                            continue;
                        }

                        try {
                            // Build Action objects from parsed API response Object.
                            process_start = System.currentTimeMillis();
                            final ActionsBuilder actionsBuilder = new ActionsBuilder(actionFactory, issue, startDate, endDate);
                            final List<Action> preFilteredActions = actionsBuilder.buildActions();
                            final List<Action> actions = getActionsFilterByLastSeen(seenIssues, issue, preFilteredActions);
                            actions.stream()
                                    .map(action -> action.getCustomFieldValues().entrySet())
                                    .flatMap(Set::stream)
                                    .filter(v -> StringUtils.isNotEmpty(v.getValue().getFormattedValue()))
                                    .map(Map.Entry::getKey)
                                    .forEach(customFieldsSeen::add);

                            process_end = System.currentTimeMillis();
                            processTime += process_end - process_start;

                            // Set built actions to actions list.
                            final long file_start = System.currentTimeMillis();
                            writer.writeActions(actions);
                            final long file_end = System.currentTimeMillis();
                            fileTime += file_end - file_start;

                            if(!firstPass // Don't bail out the first time through
                                    && preFilteredActions.size() > 0 // It had issues in our time range; so we can tell if it was filtered
                                    && actions.size() == 0) { // There is nothing new since the last time we saw it
                                reFoundTheBeginning = true;
                                break;
                            }
                            if(preFilteredActions.size() > 0) {
                                firstIssue = false;
                            }
                        } catch (final Exception e) {
                            log.error(String.format("Error parsing actions for issue %s.", issue.key), e);
                        }
                    }
                    end = System.currentTimeMillis();
                    log.trace(String.format("%d ms to get actions from a set of issues.", end - start));

                    // Otherwise we'd do another entire pass of the dataset
                    if(reFoundTheBeginning) {
                        break;
                    }
                }
                issuesAPICaller.reset();
                firstPass = false;
                log.info("Starting over to pick up lost issues.");
            }

            log.debug(String.format("Had to look up %d users.", userLookupService.numLookups()));

            final Set<CustomFieldDefinition> missedFieldDefinitions =
                    Sets.difference(
                            ImmutableSet.copyOf(config.getCustomFields()),
                            customFieldsSeen.build()
                    );
            final List<String> missedFields = missedFieldDefinitions.stream()
                    .map(CustomFieldDefinition::getName)
                    .collect(Collectors.toList());

            log.debug("No values seen for these custom fields: " + missedFields);

            start = System.currentTimeMillis();
            // Create and Upload a TSV file.
            writer.uploadTsvFile();
            end = System.currentTimeMillis();
            log.debug(String.format("%d ms to create and upload TSV.", end - start));
            fileTime += end - start;

            end_total = System.currentTimeMillis();

            final long apiUserTime = userLookupService.getUserLookupTotalTime();

            log.info(String.format("%d ms for the whole process.", end_total - start_total));
            log.info(String.format("apiTime: %dms, processTime: %dms, fileTime: %dms, userLookupTime: %dms",
                    apiTime-apiUserTime, processTime, fileTime, apiUserTime));
        } catch (final Exception e) {
            log.error("Threw an exception trying to run the index builder", e);
            throw e;
        }
    }

    private void initializeIssuesApiCaller(final IssuesAPICaller issuesAPICaller) throws IOException {
        final long start = System.currentTimeMillis();
        final int total = issuesAPICaller.setNumTotal();
        final long end = System.currentTimeMillis();
        log.debug(String.format("%d ms, found %d total issues.", end - start, total));
    }

    /*
     * We've changed the way this works, and now we could see an issue more than once (I guess technically we always
     * could, but it was far rarer). If we write the same row to the TSV more than once, we'll have duplicate data. To
     * prevent this, we'll keep track of the last timestamp for each issue, and filter out anything that's before that
     * timestamp if we see an issue again.
     *
     * ATTENTION: Requires that actions be sorted by timestamp, ascending.
     */
    @VisibleForTesting
    protected static List<Action> getActionsFilterByLastSeen(final Map<String, DateTime> seenIssues, final Issue issue,
                                                             final List<Action> actions) {
        if(actions.size() == 0) {
            return actions;
        }

        final List<Action> output;
        if(!seenIssues.containsKey(issue.key)) {
            seenIssues.remove(issue.key); // Need to change its ordering in the map
            output = actions;
        } else {
            final DateTime lastActionTime = seenIssues.get(issue.key);
            // We could binary search this instead for efficiency, but I don't think it's worth the extra work right now
            output = actions.stream().filter(a -> a.getTimestamp().isAfter(lastActionTime)).collect(Collectors.toList());
        }

        final DateTime lastTimestamp = actions.get(actions.size()-1).getTimestamp();
        seenIssues.put(issue.key, lastTimestamp);
        return output;
    }
}

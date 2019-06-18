package com.indeed.jiraactions;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.indeed.jiraactions.api.response.issue.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Paginator {
    private static final Logger log = LoggerFactory.getLogger(Paginator.class);

    private final PageProvider pageProvider;
    private final DateTime startDate;

    public Paginator(final PageProvider pageProvider, final DateTime startDate) {
        this.pageProvider = pageProvider;
        this.startDate = startDate;
    }

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
    public void process() throws InterruptedException {
        final Map<String, DateTime> seenIssues = new HashMap<>();
        boolean reFoundTheBeginning = false;
        boolean firstIssue = true;
        boolean firstPass = true;
        while (!reFoundTheBeginning || !firstIssue) {
            reFoundTheBeginning = false;
            firstIssue = true;
            final Set<String> seenThisLoop = new HashSet<>();

            while (pageProvider.hasPage()) {
                final Stopwatch stopwatch = Stopwatch.createStarted();
                final List<Issue> issues = Lists.newArrayList(pageProvider.getPage());
                log.debug(String.join(", ", issues.stream().map(x -> x.key).collect(Collectors.toList())));
                for(final Issue issue : issues) {
                    try {
                        final List<Action> preFilteredActions = pageProvider.getActions(issue);
                        final List<Action> actions = getActionsFilterByLastSeen(seenIssues, issue, preFilteredActions);
                        final List<Action> filteredActions = actions.stream().filter(a -> a.isBefore(startDate)).collect(Collectors.toList());
                        log.debug(String.valueOf(filteredActions));
                        pageProvider.writeActions(filteredActions);


                        final boolean ignoreForEndDetection = ignoreUpdatedDate(issue, preFilteredActions);
                        if(!firstPass // Don't bail out the first time through
                                && preFilteredActions.size() > 0 // It had issues in our time range; so we can tell if it was filtered
                                && actions.size() == 0// There is nothing new since the last time we saw it
                                && !ignoreForEndDetection // Ignore out of order issues
                                && !seenThisLoop.contains(issue.key) // Ignore if we see it and a few things push it down into our page
                             ) {
                            log.debug("Saw no new actions for {}, stopping.", issue.key);
                            reFoundTheBeginning = true;
                            break;
                        }
                        seenThisLoop.add(issue.key);
                        if(preFilteredActions.size() > 0 && !ignoreForEndDetection) {
                            firstIssue = false;
                        }
                    } catch (final Exception e) {
                        log.error("Error parsing actions for issue {}.", issue.key, e);
                    }
                }

                stopwatch.stop();
                log.trace("{} ms to get actions from a set of issues.", stopwatch.elapsed(TimeUnit.MILLISECONDS));

                // Otherwise we'd do another entire pass of the dataset
                if(reFoundTheBeginning) {
                    break;
                }
            }
            break;
//            pageProvider.reset();
//            firstPass = false;
//            log.info("Starting over to pick up lost issues.");
        }
    }

    /**
     * Jira sorts things by the lastUpdatedDate. That doesn't always correspond to the timestamp of an action. This
     * could happen because there's an update that's not visible (for example, a restricted visibility comment) or
     * because an existing action was modified (for example, editing a comment).
     * We retrieve the list of issues from Jira in descending updatedDate order. A comment with an action we can't
     * see could cause us to prematurely think we've finished processing all the new updates. To avoid this, we should
     * ignore things that have an updatedDate later than the last action.
     * not updates.
     *
     * ATTENTION: Requires that actions be sorted by timestamp, ascending.
     */
    protected static boolean ignoreUpdatedDate(final Issue issue, final List<Action> actions) {
        return issue.fields.updated.isAfter(actions.get(actions.size()-1).getTimestamp());
    }

    /**
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
            output = actions;
        } else {
            final DateTime lastActionTime = seenIssues.remove(issue.key); // Need to change its ordering in the map
            // We could binary search this instead for efficiency, but I don't think it's worth the extra work right now
            output = actions.stream().filter(a -> a.getTimestamp().isAfter(lastActionTime)).collect(Collectors.toList());
        }

        final DateTime lastTimestamp = actions.get(actions.size()-1).getTimestamp();
        seenIssues.put(issue.key, lastTimestamp);
        return output;
    }
}

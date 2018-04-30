package com.indeed.jiraactions;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.indeed.jiraactions.api.ApiUserLookupService;
import com.indeed.jiraactions.api.IssuesAPICaller;
import com.indeed.jiraactions.api.customfields.CustomFieldApiParser;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.links.LinkTypesApiCaller;
import com.indeed.util.logging.Loggers;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class JiraActionsIndexBuilder {
    private static final Logger log = Logger.getLogger(JiraActionsIndexBuilder.class);

    private final JiraActionsIndexBuilderConfig config;

    public JiraActionsIndexBuilder(final JiraActionsIndexBuilderConfig config) {
        this.config = config;
    }

    public void run() throws Exception {
        try {
            final Stopwatch stopwatch = Stopwatch.createStarted();

            final ApiUserLookupService userLookupService = new ApiUserLookupService(config);
            final CustomFieldApiParser customFieldApiParser = new CustomFieldApiParser(userLookupService);
            final ActionFactory actionFactory = new ActionFactory(userLookupService, customFieldApiParser, config);

            final IssuesAPICaller issuesAPICaller = new IssuesAPICaller(config);
            initializeIssuesApiCaller(issuesAPICaller);

            if(!issuesAPICaller.currentPageExist()) {
                log.warn("No issues found for this time range.");
                return;
            }

            long fileTime = 0;

            final DateTime startDate = JiraActionsUtil.parseDateTime(config.getStartDate());
            final DateTime endDate = JiraActionsUtil.parseDateTime(config.getEndDate());

            if (!startDate.isBefore(endDate)) {
                Loggers.error(log, "Invalid start date '%s' not before end date '%s'", startDate, endDate);
            }

            final LinkTypesApiCaller linkTypesApiCaller = new LinkTypesApiCaller(config);
            final List<String> linkTypes = linkTypesApiCaller.getLinkTypes();

            final TsvFileWriter writer = new TsvFileWriter(config, linkTypes);
            final Stopwatch headerStopwatch = Stopwatch.createStarted();
            writer.createFileAndWriteHeaders();
            headerStopwatch.stop();
            fileTime += headerStopwatch.elapsed(TimeUnit.MILLISECONDS);

            final ApiPageProvider apiPageProvider = new ApiPageProvider(issuesAPICaller, actionFactory, config, writer);
            final Paginator paginator = new Paginator(apiPageProvider, startDate, endDate);

            paginator.process();
            fileTime += apiPageProvider.getFileWritingTime();
            final long apiTime = apiPageProvider.getApiTime();
            final long processTime = apiPageProvider.getProcessingTime();

            log.debug(String.format("Had to look up %d users.", userLookupService.numLookups()));

            final Set<CustomFieldDefinition> missedFieldDefinitions =
                    Sets.difference(
                            ImmutableSet.copyOf(config.getCustomFields()),
                            apiPageProvider.getCustomFieldsSeen()
                    );
            final List<String> missedFields = missedFieldDefinitions.stream()
                    .map(CustomFieldDefinition::getName)
                    .collect(Collectors.toList());

            log.debug("No values seen for these custom fields: " + missedFields);

            final Stopwatch fileUploadStopwatch = Stopwatch.createStarted();
            writer.uploadTsvFile();
            fileUploadStopwatch.stop();
            Loggers.debug(log, "%d ms to create and upload TSV.", fileUploadStopwatch.elapsed(TimeUnit.MILLISECONDS));

            stopwatch.stop();

            final long apiUserTime = userLookupService.getUserLookupTotalTime();

            log.info(String.format("%d ms for the whole process.", stopwatch.elapsed(TimeUnit.MILLISECONDS)));
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
}

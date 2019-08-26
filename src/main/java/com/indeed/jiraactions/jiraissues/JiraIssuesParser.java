package com.indeed.jiraactions.jiraissues;

import com.google.common.base.Stopwatch;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class JiraIssuesParser {
    private static final Logger log = LoggerFactory.getLogger(JiraIssuesParser.class);
    private static TsvParserSettings settings = setupSettings();

    private final JiraIssuesProcess process;
    private final JiraIssuesFileWriter fileWriter;

    private final List<String> newFields;
    private final List<String[]> newIssues;
    private File file;
    private FileReader reader;
    private TsvParser parser;

    public JiraIssuesParser(final JiraIssuesFileWriter fileWriter, final JiraIssuesProcess process, final List<String> newFields, final List<String[]> newIssues) {
        this.fileWriter = fileWriter;
        this.process = process;
        this.newFields = newFields;
        this.newIssues = newIssues;
    }

    private static TsvParserSettings setupSettings() {
        final TsvParserSettings settings = new TsvParserSettings();
        settings.getFormat().setLineSeparator("\n");
        settings.setMaxColumns(1000);
        settings.setMaxCharsPerColumn(10000);
        settings.setNullValue("");
        return settings;
    }

    public void setupParserAndProcess() throws FileNotFoundException {
        file = new File("jiraissues_downloaded.tsv");
        reader = new FileReader(file);
        parser = new TsvParser(settings);
        parser.beginParsing(reader);

        fileWriter.setFields(newFields);

        process.setNewFields(newFields);
        process.setNewIssues(newIssues);
        process.setOldFields(Arrays.stream(parser.parseNext()).collect(Collectors.toList()));
        process.convertToMap();
    }

    public void parseTsv() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        int counter = 0;
        while (true) {
            final String[] issue = parser.parseNext();
            if (issue == null) {
                stopwatch.stop();
                break;
            } else {
                final Map<String, String> processedIssue = process.compareAndUpdate(issue);
                if (processedIssue != null) {
                    fileWriter.writeIssue(processedIssue);
                }
                counter++;
                if (counter % 100000 == 0) {
                    log.debug("{} ms to parse {} issues.", stopwatch.elapsed(TimeUnit.MILLISECONDS), counter);
                }
            }
        }
        log.debug("Updated/Replaced {} Issues.", counter);
        if (!process.getNonApiStatuses().isEmpty()) {
            log.warn("Fields not in API {}", process.getNonApiStatuses());
        }
        for (final Map<String, String> issue : process.getRemainingIssues()) {     // Adds the remaining issues
            fileWriter.writeIssue(issue);
        }
    }
}

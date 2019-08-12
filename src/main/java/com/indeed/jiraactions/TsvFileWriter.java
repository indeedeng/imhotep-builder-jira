package com.indeed.jiraactions;

import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class TsvFileWriter {
    private static final Logger log = LoggerFactory.getLogger(TsvFileWriter.class);

    private final JiraActionsIndexBuilderConfig config;
    private final Map<DateMidnight, WriterData> writerDataMap;
    private final List<TSVColumnSpec> columnSpecs;
    private final List<TSVColumnSpec> columnSpecsJiraissues;
    private final List<String[]> issues = new ArrayList<>();

    public TsvFileWriter(final JiraActionsIndexBuilderConfig config, final List<String> linkTypes, final List<String> statusTypes) {
        this.config = config;
        final int days = Days.daysBetween(JiraActionsUtil.parseDateTime(config.getStartDate()),
                JiraActionsUtil.parseDateTime(config.getEndDate())).getDays();
        writerDataMap = new HashMap<>(days);
        this.columnSpecs = createColumnSpecs(linkTypes);
        this.columnSpecsJiraissues = createColumnSpecsJiraissues(linkTypes, statusTypes);
    }

    private static final String FILENAME_DATE_TIME_PATTERN = "yyyyMMdd";
    private String reformatDate(final DateTime date) {
        return date.toString(FILENAME_DATE_TIME_PATTERN);
    }

    public void createFileAndWriteHeaders() throws IOException {
        final DateTime endDate = JiraActionsUtil.parseDateTime(config.getEndDate());
        for(DateTime date = JiraActionsUtil.parseDateTime(config.getStartDate()); date.isBefore(endDate); date = date.plusDays(1)) {
            createFileAndWriteHeaders(date);
            setJiraissuesHeaders();
        }
    }

    public List<String[]> getIssues() {
        return issues;
    }

    private List<TSVColumnSpec> createColumnSpecs(final List<String> linkTypes) {
        final TSVSpecBuilder specBuilder = new TSVSpecBuilder();
        specBuilder
                .addColumn("issuekey", Action::getIssuekey)
                .addColumn("action", Action::getAction)
                .addUserColumns("actor", Action::getActor)
                .addUserColumns("assignee", Action::getAssignee)
                .addColumn("category", Action::getCategory)
                .addColumn("components*|", Action::getComponents)
                .addColumn("createdate", Action::getCreatedDate)
                .addColumn("duedate", Action::getDueDate)
                .addTimeColumn("int duedate_time", Action::getDueDateTime)
                .addColumn("fieldschanged*", Action::getFieldschanged)
                .addColumn("fixversion*|", Action::getFixversions)
                .addLongColumn("issueage", Action::getIssueage)
                .addColumn("issuetype", Action::getIssuetype)
                .addColumn("labels*", Action::getLabels)
                .addColumn("priority", Action::getPriority)
                .addColumn("project", Action::getProject)
                .addColumn("projectkey", Action::getProjectkey)
                .addColumn("prevstatus", Action::getPrevstatus)
                .addUserColumns("reporter", Action::getReporter)
                .addColumn("resolution", Action::getResolution)
                .addColumn("status", Action::getStatus)
                .addColumn("summary", Action::getSummary)
                .addLongColumn("timeinstate", Action::getTimeinstate)
                .addLongColumn("timesinceaction", Action::getTimesinceaction)
                .addTimeColumn("time", Action::getTimestamp)
                .addLinkColumns(linkTypes);

        for (final CustomFieldDefinition customField : config.getCustomFields()) {
            specBuilder.addCustomFieldColumns(customField);
        }
        return specBuilder.build();
    }

    private List<TSVColumnSpec> createColumnSpecsJiraissues(final List<String> linkTypes, final List<String> statusTypes) {
        final TSVSpecBuilder specBuilder = new TSVSpecBuilder();
        specBuilder
                .addColumn("issuekey", Action::getIssuekey)
                .addUserColumns("actor", Action::getActor)
                .addUserColumns("assignee", Action::getAssignee)
                .addColumn("category", Action::getCategory)
                .addColumn("components*|", Action::getComponents)
                .addLongColumn("createdate", Action::getCreatedDateLong)
                .addColumn("duedate", Action::getDueDate)
                .addTimeColumn("int duedate_time", Action::getDueDateTime)
                .addColumn("fixversion*|", Action::getFixversions)
                .addLongColumn("issueage", Action::getIssueage)
                .addColumn("issuetype", Action::getIssuetype)
                .addColumn("labels*", Action::getLabels)
                .addColumn("priority", Action::getPriority)
                .addColumn("project", Action::getProject)
                .addColumn("projectkey", Action::getProjectkey)
                .addUserColumns("reporter", Action::getReporter)
                .addColumn("resolution", Action::getResolution)
                .addColumn("status", Action::getStatus)
                .addColumn("summary", Action::getSummary)
                .addTimeColumn("time", Action::getTimestamp)
                .addLongColumn("comments", Action::getComments)
                .addLongColumn("closedate", Action::getClosedDate)
                .addLongColumn("resolvedate", Action::getResolvedDate)
                .addLongColumn("lastupdated", Action::getLastUpdated)
                .addLongColumn("delivery_lead_time", Action::getDeliveryLeadTime)
                .addStatusTimeColumns(statusTypes)
                .addLinkColumns(linkTypes);

        for (final CustomFieldDefinition customField : config.getCustomFields()) {
            specBuilder.addCustomFieldColumns(customField);
        }
        return specBuilder.build();
    }

    private void createFileAndWriteHeaders(final DateTime day) throws IOException {
        final String filename = String.format("%s_%s.tsv", config.getIndexName(), reformatDate(day));
        final File file = new File(filename);
        if (StringUtils.isNotEmpty(config.getIuploadURL())) {
            file.deleteOnExit();
        } else {
            log.info("Not deleting tsv file because upload url is unset.");
        }

        final BufferedWriter bw = new BufferedWriter(new FileWriter(file));

        final String headerLine = columnSpecs.stream()
                .map(TSVColumnSpec::getHeader)
                .collect(Collectors.joining("\t"));

        // Write header
        bw.write(headerLine);
        bw.newLine();
        bw.flush();

        writerDataMap.put(day.toDateMidnight(), new WriterData(file, bw));
    }

    public void writeActions(final List<Action> actions) throws IOException {
        if(actions.isEmpty()) {
            return;
        }

        for (final Action action : actions) {
            final WriterData writerData = writerDataMap.get(action.getTimestamp().toDateMidnight());
            final BufferedWriter bw = writerData.getBufferedWriter();
            writerData.setWritten();
            writerData.setDirty(true);
            final String line = columnSpecs.stream()
                    .map(columnSpec -> columnSpec.getActionExtractor().apply(action))
                    .map(rawValue -> rawValue.replace("\t", "\\t"))
                    .map(rawValue -> rawValue.replace("\n", "\\n"))
                    .map(rawValue -> rawValue.replace("\r", "\\r"))
                    .collect(Collectors.joining("\t"));
            bw.write(line);
            bw.newLine();
        }

        writerDataMap.values().stream()
                .filter(WriterData::isDirty).forEach(x -> {
            try {
                x.getBufferedWriter().flush();
                x.setDirty(false);
            } catch (final IOException e) {
                log.error("Failed to flush.", e);
            }
        });
    }

    private void setJiraissuesHeaders() {
        final String[] headerLine = columnSpecsJiraissues.stream()
                .map(TSVColumnSpec::getHeader)
                .toArray(String[]::new);
        issues.add(headerLine);
    }

    public void writeIssue(final Action action) throws IOException {
        if(action == null) {
            return;
        }
        final String[] line = columnSpecsJiraissues.stream()
                .map(columnSpec -> columnSpec.getActionExtractor().apply(action))
                .map(rawValue -> rawValue.replace("\t", "\\t"))
                .map(rawValue -> rawValue.replace("\n", "\\n"))
                .map(rawValue -> rawValue.replace("\r", "\\r"))
                .toArray(String[]::new);
        issues.add(line);
    }

    private static final int NUM_RETRIES = 5;
    public void uploadTsvFile() {
        if (StringUtils.isEmpty(config.getIuploadURL())) {
            log.info("Skipping upload because iuploadurl is empty.");
            return;
        }

        final String iuploadUrl = String.format("%s/%s/file/", config.getIuploadURL(), config.getIndexName());

        log.info("Uploading to " + iuploadUrl);

        final String userPass = config.getIuploadUsername() + ":" + config.getIuploadPassword();
        final String basicAuth = "Basic " + new String(new Base64().encode(userPass.getBytes()));

        writerDataMap.values().forEach(wd -> {
            try {
                wd.getBufferedWriter().close();
            } catch (final IOException e) {
                log.error("Failed to close " + wd.file.getName() + ".", e);
            }

            if (wd.isWritten()) {
                final File file = wd.getFile();
                final HttpPost httpPost = new HttpPost(iuploadUrl);
                httpPost.setHeader("Authorization", basicAuth);
                httpPost.setEntity(MultipartEntityBuilder.create()
                        .addBinaryBody("file", file, ContentType.MULTIPART_FORM_DATA, file.getName())
                        .build());

                for(int i = 0; i < NUM_RETRIES; i++) {
                    try {
                        final HttpResponse response = HttpClientBuilder.create().build().execute(httpPost);
                        log.info("Http response: " + response.getStatusLine().toString() + ": " + wd.file.getName() + ".");
                        if(response.getStatusLine().getStatusCode() != 200) {
                            continue;
                        }
                        return;
                    } catch (final IOException e) {
                        log.warn("Failed to upload file: " + wd.file.getName() + ".", e);
                    }
                }
                log.error("Retries expired, unable to upload file: " + wd.file.getName() + ".");
            }
        });
    }

    private static class WriterData {
        private final File file;
        private final BufferedWriter bw;
        private boolean written = false;
        private boolean dirty = false;

        private WriterData(final File file, final BufferedWriter bw) {
            this.file = file;
            this.bw = bw;
        }

        private File getFile() {
            return file;
        }

        private BufferedWriter getBufferedWriter() {
            return bw;
        }

        private boolean isWritten() {
            return written;
        }

        private void setWritten() {
            this.written = true;
        }

        private boolean isDirty() {
            return dirty;
        }

        private void setDirty(final boolean dirty) {
            this.dirty = dirty;
        }
    }
}

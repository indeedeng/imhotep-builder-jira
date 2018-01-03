package com.indeed.jiraactions;

import com.google.common.base.Joiner;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.customfields.CustomFieldValue;
import com.indeed.util.logging.Loggers;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TsvFileWriter {
    private static final Logger log = Logger.getLogger(TsvFileWriter.class);

    private final JiraActionsIndexBuilderConfig config;
    private final Map<DateMidnight, WriterData> writerDataMap;

    private static final String[] FILE_HEADER = {
        "action", "actor", "actorusername", "assignee", "assigneeusername", "category", "components*|", "createddate", "duedate",
            "int duedate_time", "fieldschanged*", "fixversion*|", "issueage", "issuekey", "issuetype", "labels*", "priority",
            "project", "projectkey", "prevstatus", "reporter", "reporterusername", "resolution", "status", "summary", "timeinstate",
            "timesinceaction", "time"
    };

    public TsvFileWriter(final JiraActionsIndexBuilderConfig config) {
        this.config = config;
        final int days = Days.daysBetween(JiraActionsUtil.parseDateTime(config.getStartDate()),
                JiraActionsUtil.parseDateTime(config.getEndDate())).getDays();
        writerDataMap = new HashMap<>(days);
    }

    private static final String FILENAME_DATE_TIME_PATTERN = "yyyyMMdd";
    private String reformatDate(final DateTime date) {
        return date.toString(FILENAME_DATE_TIME_PATTERN);
    }

    public void createFileAndWriteHeaders() throws IOException {
        final DateTime endDate = JiraActionsUtil.parseDateTime(config.getEndDate());
        for(DateTime date = JiraActionsUtil.parseDateTime(config.getStartDate()); date.isBefore(endDate); date = date.plusDays(1)) {
            createFileAndWriteHeaders(date);
        }
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

        // Write header
        bw.write(Joiner.on("\t").join(FILE_HEADER));

        for(final CustomFieldDefinition customField : config.getCustomFields()) {
            bw.write("\t");
            customField.writeHeader(bw);
        }
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
            bw.write(action.getAction());
            bw.write("\t");
            bw.write(action.getActor());
            bw.write("\t");
            bw.write(action.getActorusername());
            bw.write("\t");
            bw.write(action.getAssignee());
            bw.write("\t");
            bw.write(action.getAssigneeusername());
            bw.write("\t");
            bw.write(action.getCategory());
            bw.write("\t");
            bw.write(action.getComponents());
            bw.write("\t");
            bw.write(action.getCreatedDate());
            bw.write("\t");
            bw.write(action.getDueDate());
            bw.write("\t");
            bw.write(JiraActionsUtil.getUnixTimestamp(action.getDueDateTime()));
            bw.write("\t");
            bw.write(action.getFieldschanged());
            bw.write("\t");
            bw.write(action.getFixversions().replace(Character.toString('\t'), "<tab>"));
            bw.write("\t");
            bw.write(String.valueOf(action.getIssueage()));
            bw.write("\t");
            bw.write(action.getIssuekey());
            bw.write("\t");
            bw.write(action.getIssuetype());
            bw.write("\t");
            bw.write(action.getLabels());
            bw.write("\t");
            bw.write(action.getPriority());
            bw.write("\t");
            bw.write(action.getProject());
            bw.write("\t");
            bw.write(action.getProjectkey());
            bw.write("\t");
            bw.write(action.getPrevstatus());
            bw.write("\t");
            bw.write(action.getReporter());
            bw.write("\t");
            bw.write(action.getReporterusername());
            bw.write("\t");
            bw.write(action.getResolution().replace(Character.toString('\t'), "<tab>"));
            bw.write("\t");
            bw.write(action.getStatus());
            bw.write("\t");
            bw.write(action.getSummary().replace(Character.toString('\t'), "<tab>"));
            bw.write("\t");
            bw.write(String.valueOf(action.getTimeinstate()));
            bw.write("\t");
            bw.write(String.valueOf(action.getTimesinceaction()));
            bw.write("\t");
            bw.write(JiraActionsUtil.getUnixTimestamp(action.getTimestamp()));

            for(final CustomFieldDefinition customField : config.getCustomFields()) {
                bw.write("\t");
                final CustomFieldValue value = action.getCustomFieldValues().get(customField);
                if(value == null) {
                    Loggers.error(log, "No value found for custom field %s for issue %s", customField.getImhotepFieldName(), action.getIssuekey());
                    CustomFieldValue.emptyCustomField(customField).writeValue(bw);
                } else {
                    value.writeValue(bw);
                }
            }
            bw.newLine();
        }

        writerDataMap.values().forEach(x -> {
            try {
                x.getBufferedWriter().flush();
            } catch (final IOException e) {
                log.error("Failed to flush.", e);
            }
        });
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
        private boolean written;

        private WriterData(final File file, final BufferedWriter bw) {
            this.file = file;
            this.bw = bw;
            this.written = false;
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
    }
}

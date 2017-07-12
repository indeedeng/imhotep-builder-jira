package com.indeed.jiraactions;

import com.google.common.base.Joiner;
import org.apache.commons.codec.binary.Base64;
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
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author soono, kbinswanger
 */
public class TsvFileWriter {
    private static final Logger log = Logger.getLogger(TsvFileWriter.class);

    private final JiraActionsIndexBuilderConfig config;
    private final Map<DateMidnight, WriterData> writerDataMap;

    private static final String[] FILE_HEADER = {
        "action", "actor", "actorusername", "assignee", "assigneeusername", "category", "components*|", "duedate",
            "int duedate_time", "fieldschanged*", "fixversion*|", "issueage", "issuekey", "issuetype", "labels*", "project",
            "projectkey", "prevstatus", "reporter", "reporterusername", "resolution", "status", "summary", "timeinstate",
            "timesinceaction", "time"
    };

    private static final String[] CUSTOM_HEADERS = {
            "verifier", "verifierusername", "issuesizeestimate", "evnt_directcause", "sprints*|", "sysad_category1",
            "sysad_category2", "millistorypoints"
    };


    public TsvFileWriter(final JiraActionsIndexBuilderConfig config) throws IOException {
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
        file.deleteOnExit();

        final BufferedWriter bw = new BufferedWriter(new FileWriter(file));

        // Write header
        final StringBuilder headers = new StringBuilder(Joiner.on("\t").join(FILE_HEADER));
        if(!config.isIgnoreCustomFields()) {
            headers.append("\t")
                    .append(Joiner.on("\t").join(CUSTOM_HEADERS));
        }
        bw.write(headers.toString());
        bw.newLine();
        bw.flush();

        writerDataMap.put(day.toDateMidnight(), new WriterData(file, bw));
    }

    public void writeActions(final List<Action> actions) throws IOException, ParseException {
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
            bw.write(action.getResolution());
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

            if(!config.isIgnoreCustomFields()) {
                bw.write("\t");
                bw.write(action.getVerifier());
                bw.write("\t");
                bw.write(action.getVerifierusername());
                bw.write("\t");
                bw.write(action.getIssueSizeEstimate());
                bw.write("\t");
                bw.write(action.getDirectCause());
                bw.write("\t");
                bw.write(action.getSprints());
                bw.write("\t");
                bw.write(action.getSysadCategories1());
                bw.write("\t");
                bw.write(action.getSysadCategories2());
                bw.write("\t");
                bw.write(action.getMilliStoryPoints());
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
    public void uploadTsvFile() throws IOException {
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
                        return;
                    } catch (final IOException e) {
                        log.warn("Failed to upload file: " + wd.file.getName() + ".", e);
                    }
                }
                log.error("Retries expired, unable to upload file: " + wd.file.getName() + ".");
            }
        });
    }

    private class WriterData {
        private final File file;
        private final BufferedWriter bw;
        private boolean written;

        public WriterData(final File file, final BufferedWriter bw) {
            this.file = file;
            this.bw = bw;
            this.written = false;
        }

        public File getFile() {
            return file;
        }

        public BufferedWriter getBufferedWriter() {
            return bw;
        }

        public boolean isWritten() {
            return written;
        }

        public void setWritten() {
            this.written = true;
        }
    }
}

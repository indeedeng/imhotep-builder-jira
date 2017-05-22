package com.indeed.skeleton.index.builder.jiraaction;

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

    private final JiraActionIndexBuilderConfig config;
    private final Map<DateMidnight, WriterData> writerDataMap;

    private static final String[] FILE_HEADER = {
        "action", "actor", "assignee", "category", "components*|", "duedate", "fieldschanged*", "fixversion*|", "issueage",
            "issuekey", "issuetype", "labels*", "project", "prevstatus", "reporter", "resolution", "status", "summary",
            "timeinstate", "timesinceaction", "time", "verifier", "issuesizeestimate", "directcause"
    };


    public TsvFileWriter(final JiraActionIndexBuilderConfig config) {
        this.config = config;
        final int days = Days.daysBetween(JiraActionUtil.parseDateTime(config.getStartDate()),
                JiraActionUtil.parseDateTime(config.getEndDate())).getDays();
        writerDataMap = new HashMap<>(days);
    }

    private static final String FILENAME_DATE_TIME_PATTERN = "yyyyMMdd";
    private String reformatDate(final String date) {
        final DateTime dateTime = JiraActionUtil.parseDateTime(date);
        return dateTime.toString(FILENAME_DATE_TIME_PATTERN);
    }

    private String reformatDate(final DateTime date) {
        return date.toString(FILENAME_DATE_TIME_PATTERN);
    }

    public void createFileAndWriteHeaders() throws IOException {
        final DateTime endDate = JiraActionUtil.parseDateTime(config.getEndDate());
        for(DateTime date = JiraActionUtil.parseDateTime(config.getStartDate()); date.isBefore(endDate); date = date.plusDays(1)) {
            createFileAndWriteHeaders(date);
        }
    }

    private void createFileAndWriteHeaders(final DateTime day) throws IOException {
        final String filename = String.format("%s_%s.tsv", config.getIndexName(), reformatDate(day));
        final File file = new File(filename);

        final BufferedWriter bw = new BufferedWriter(new FileWriter(file));

        boolean hasWritten = false;
        // Write header
        for (final String header : FILE_HEADER) {
            if(config.isIgnoreCustomFields() && ("verifier".equals(header) || "issuesizeestimate".equals(header) || "directCause".equals(header))) {
                continue;
            }
            if (hasWritten) {
                bw.write("\t");
            }
            bw.write(header);
            hasWritten = true;
        }
        bw.newLine();
        bw.flush();

        writerDataMap.put(day.toDateMidnight(), new WriterData(file, bw));
    }

    public void writeActions(final List<Action> actions) throws IOException, ParseException {
        if(actions.isEmpty()) {
            return;
        }

        for (final Action action : actions) {
            final WriterData writerData = writerDataMap.get(action.timestamp.toDateMidnight());
            final BufferedWriter bw = writerData.getBufferedWriter();
            writerData.setWritten(true);
            bw.write(action.action);
            bw.write("\t");
            bw.write(action.actor);
            bw.write("\t");
            bw.write(action.assignee);
            bw.write("\t");
            bw.write(action.category);
            bw.write("\t");
            bw.write(action.components);
            bw.write("\t");
            bw.write(action.dueDate);
            bw.write("\t");
            bw.write(action.fieldschanged);
            bw.write("\t");
            bw.write(action.fixversions.replace(Character.toString('\t'), "<tab>"));
            bw.write("\t");
            bw.write(String.valueOf(action.issueage));
            bw.write("\t");
            bw.write(action.issuekey);
            bw.write("\t");
            bw.write(action.issuetype);
            bw.write("\t");
            bw.write(action.labels);
            bw.write("\t");
            bw.write(action.project);
            bw.write("\t");
            bw.write(action.prevstatus);
            bw.write("\t");
            bw.write(action.reporter);
            bw.write("\t");
            bw.write(action.resolution);
            bw.write("\t");
            bw.write(action.status);
            bw.write("\t");
            bw.write(action.summary.replace(Character.toString('\t'), "<tab>"));
            bw.write("\t");
            bw.write(String.valueOf(action.timeinstate));
            bw.write("\t");
            bw.write(String.valueOf(action.timesinceaction));
            bw.write("\t");
            bw.write(JiraActionUtil.getUnixTimestamp(action.timestamp));

            if(!config.isIgnoreCustomFields()) {
                bw.write("\t");
                bw.write(action.verifier);
                bw.write("\t");
                bw.write(action.issueSizeEstimate);
                bw.write("\t");
                bw.write(action.directCause);
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

        public void setWritten(final boolean written) {
            this.written = written;
        }
    }
}

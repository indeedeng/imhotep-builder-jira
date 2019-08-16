package com.indeed.jiraactions.jiraissues;

import com.indeed.jiraactions.JiraActionsIndexBuilderConfig;
import com.indeed.jiraactions.JiraActionsUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class JiraIssuesFileWriter {
    private static final Logger log = LoggerFactory.getLogger(JiraIssuesFileWriter.class);
    private static final int NUM_RETRIES = 5;

    private final JiraActionsIndexBuilderConfig config;

    private WriterData writerData;
    private List<String> fields = new ArrayList<>();

    public JiraIssuesFileWriter(final JiraActionsIndexBuilderConfig config) {
        this.config = config;
    }

    public boolean downloadTsv() throws IOException, InterruptedException {
        int backoff = 10000;
        final DateTime date = JiraActionsUtil.parseDateTime(config.getStartDate());
        final String formattedDate = date.minusDays(1).toString("yyyyMMdd");

        final String userPass = config.getIuploadUsername() + ":" + config.getIuploadPassword();
        final String basicAuth = "Basic " + new String(new Base64().encode(userPass.getBytes()));

        final File file = new File("jiraissues_downloaded.tsv");
        file.deleteOnExit();
        final FileOutputStream stream = new FileOutputStream(file);

        for (int tries = 1; tries <= NUM_RETRIES; tries++) {
            backoff = Math.max(backoff / 2, 10000);
            final URL url = new URL(config.getIuploadURL() + "/jiraissues/file/indexed/jiraissues_" + formattedDate + ".tsv.gz/");
            final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", basicAuth);
            if (connection.getResponseCode() == 400) {
                log.info("Previous Day's TSV missing. Using API method.");
                return true;
            }

            try (final GZIPInputStream in = new GZIPInputStream(connection.getInputStream())) {
                int length;
                final byte[] buffer = new byte[1024];
                while ((length = in.read(buffer)) > -1) {
                    stream.write(buffer, 0, length);
                }
                log.info("Successfully downloaded file with {}.", url);
                stream.close();
                in.close();
                return false;
            } catch (final IOException e) {
                log.error("Failed on try {}/5.", tries);
                if (tries == 5) {
                    log.error("Failed on final try, aborting.", e);
                }
                Thread.sleep(backoff);
                backoff *= 2;
            }
        }
        return true;
    }

    public void compressAndUploadTsv() throws IOException {
        compressGzip();

        final String iuploadUrl = String.format("%s/%s/file/", config.getIuploadURL(), "jiraissues");

        log.info("Uploading to " + iuploadUrl);

        final String userPass = config.getIuploadUsername() + ":" + config.getIuploadPassword();
        final String basicAuth = "Basic " + new String(new Base64().encode(userPass.getBytes()));
        try {
            writerData.getBufferedWriter().close();
        } catch (final IOException e) {
            log.error("Failed to close" + writerData.file.getName() + ".", e);
        }

        final File file = new File(writerData.getFile().getName() + ".gz");
        if (writerData.isWritten()) {
            final HttpPost httpPost = new HttpPost(iuploadUrl);
            httpPost.setHeader("Authorization", basicAuth);
            httpPost.setEntity(MultipartEntityBuilder.create()
                    .addBinaryBody("file", file, ContentType.MULTIPART_FORM_DATA, file.getName())
                    .build());

            for (int i = 0; i < NUM_RETRIES; i++) {
                try {
                    final HttpResponse response = HttpClientBuilder.create().build().execute(httpPost);
                    log.info("Http response: " + response.getStatusLine().toString() + ": " + file.getName() + ".");
                    if (response.getStatusLine().getStatusCode() == 200) {
                        return;
                    }
                } catch (final IOException e) {
                    log.warn("Failed to upload file: " + file.getName() + ".", e);
                }
            }
            log.error("Retries expired, unable to upload file: " + file.getName() + ".");
        }
    }

    public void createTsvAndSetHeaders() throws IOException {
        final DateTime date = JiraActionsUtil.parseDateTime(config.getStartDate());
        final String formattedDate = date.toString("yyyyMMdd");
        final File file = new File("jiraissues_" + formattedDate + ".tsv");
        file.deleteOnExit();
        final BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        final String fieldsLine = String.join("\t", fields);
        bw.write(fieldsLine);
        bw.newLine();
        bw.flush();

        writerData = new WriterData(file, bw);
    }

    public void compressGzip() throws IOException {
        final byte[] buffer = new byte[1024];
        final File gzip = new File(writerData.getFile().getName() + ".gz");
        gzip.deleteOnExit();
        try {
            final FileInputStream in = new FileInputStream(writerData.getFile());
            final GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(gzip));
            int i;
            while ((i = in.read(buffer)) > 0) {
                out.write(buffer, 0, i);
            }
            in.close();
            out.finish();
            out.close();
        } catch (final IOException e) {
            throw e;
        }
    }

    public void writeIssue(final Map<String, String> issue) {
        final String line = issue.values().stream()
                .map(rawValue -> rawValue.replace("\t", "\\t"))
                .map(rawValue -> rawValue.replace("\n", "\\n"))
                .map(rawValue -> rawValue.replace("\r", "\\r"))
                .collect(Collectors.joining("\t"));
        final BufferedWriter bw = writerData.getBufferedWriter();
        writerData.setWritten();
        try {
            bw.write(line);
            bw.newLine();
            bw.flush();
        } catch (final IOException e) {
            log.error("Unable to write new line.", e);
        }
    }

    public void setFields(final List<String> fields) {
        this.fields = fields;
    }

    private static class WriterData {
        private final File file;
        private final BufferedWriter bw;
        private boolean written = false;

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
    }
}

package com.indeed.skeleton.index.builder.jiraaction;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;


/**
 * @author soono
 */
public class TsvFileWriter {
    private final JiraActionIndexBuilderConfig config;

    public static final String [] FILE_HEADER = {
        "action", "actor", "assignee", "category", "fieldschanged*", "fixversion*|", "issueage", "issuekey",
            "issuetype", "project", "prevstatus", "reporter", "resolution", "status", "summary", "timeinstate",
            "timesinceaction", "time", "verifier"
    };


    public TsvFileWriter(final JiraActionIndexBuilderConfig config) {
        this.config = config;
    }

    private static final String FILENAME_DATE_TIME_PATTERN = "yyyyMMdd.HH";
    private String reformatDate(final String date) {
        final DateTime dateTime = JiraActionUtil.parseDateTime(date);
        return dateTime.toString(FILENAME_DATE_TIME_PATTERN);
    }

    public void createTSVFile(final List<Action> actions) throws IOException, ParseException {
        final String filename = String.format("jiraactions_%s-%s.tsv", reformatDate(config.getStartDate()), reformatDate(config.getEndDate()));
        final File file = new File(filename);
        final BufferedWriter bw = new BufferedWriter(new FileWriter(file));

        // Write header
        for (int i=0; i< FILE_HEADER.length; i++) {
            if (i > 0) bw.write("\t");
            final String header = FILE_HEADER[i];
            bw.write(header);
        }
        bw.write("\n");

        // Write body
        for (final Action action : actions) {
            bw.write(action.action);
            bw.write("\t");
            bw.write(action.actor);
            bw.write("\t");
            bw.write(action.assignee);
            bw.write("\t");
            bw.write(action.category);
            bw.write("\t");
            bw.write(action.fieldschanged);
            bw.write("\t");
            bw.write(action.fixversions);
            bw.write("\t");
            final String issueage = String.valueOf(action.issueage);
            bw.write(issueage);
            bw.write("\t");
            bw.write(action.issuekey);
            bw.write("\t");
            bw.write(action.issuetype);
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
            bw.write(action.summary);
            bw.write("\t");
            final String timeinstate = String.valueOf(action.timeinstate);
            bw.write(timeinstate);
            bw.write("\t");
            final String timesinceaction = String.valueOf(action.timesinceaction);
            bw.write(timesinceaction);
            bw.write("\t");
            bw.write(JiraActionUtil.getUnixTimestamp(action.timestamp));
            bw.write("\t");
            bw.write(action.verifier);
            bw.write("\n");
        }
        bw.close();

        uploadTsvFile(file);
    }

    private void uploadTsvFile(@Nonnull final File tsvFile) throws IOException {
        final String iuploadUrl = config.getIuploadURL();

        final String userPass = config.getJiraUsernameIndexer() + ":" + config.getJiraPasswordIndexer();
        final String basicAuth = "Basic " + new String(new Base64().encode(userPass.getBytes()));

        final HttpPost httpPost = new HttpPost(iuploadUrl);
        httpPost.setHeader("Authorization", basicAuth);
        httpPost.setEntity(MultipartEntityBuilder.create()
                .addBinaryBody("file", tsvFile, ContentType.MULTIPART_FORM_DATA, tsvFile.getName())
                .build());

        HttpClientBuilder.create().build().execute(httpPost);
    }
}

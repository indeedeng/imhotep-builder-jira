package com.indeed.skeleton.index.builder.jiraaction;

import org.apache.http.client.methods.HttpPost;

import javax.annotation.Nonnull;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.commons.codec.binary.Base64;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * Created by soono on 9/8/16.
 */
public class TsvFileWriter {
    public static final String [] FILE_HEADER = {
        "action", "actor", "assignee", "fieldschanged*", "issueage", "issuekey", "issuetype", "project", "prevstatus", "reporter",
            "resolution", "status", "summary", "timeinstate", "time", "verifier"
    };
    public static void createTSVFile(List<Action> actions) throws IOException, ParseException {
        String filename = "jiraactions_" + getYesterday() + ".tsv";
        File file = new File(filename);
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));

        // Write header
        for (int i=0; i< FILE_HEADER.length; i++) {
            if (i > 0) bw.write("\t");
            String header = FILE_HEADER[i];
            bw.write(header);
        }
        bw.write("\n");

        // Write body
        for (Action action : actions) {
            bw.write(action.action);
            bw.write("\t");
            bw.write(action.actor);
            bw.write("\t");
            bw.write(action.assignee);
            bw.write("\t");
            bw.write(action.fieldschanged);
            bw.write("\t");
            String issueage = String.valueOf(action.issueage);
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
            String timeinstate = String.valueOf(action.timeinstate);
            bw.write(timeinstate);
            bw.write("\t");
            bw.write(getUnixTimestamp(action.timestamp));
            bw.write("\t");
            bw.write(action.verifier);
            bw.write("\n");
        }
        bw.close();

        uploadTsvFile(file);
    }

    private static String getYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return sdf.format(cal.getTime());
    }

    private static String getUnixTimestamp(String jiraTimestamp) throws ParseException {
        String timestamp = jiraTimestamp.replace('T', ' ');
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = dateFormat.parse(timestamp);
        long unixTime = date.getTime()/1000;
        return String.valueOf(unixTime);
    }

    private static void uploadTsvFile(@Nonnull final File tsvFile) throws IOException {
        final ConfigReader configReader = new PropertiesConfigReader();

        final String iuploadUrl = configReader.iuploadURL();

        final String userPass = configReader.username() + ":" + configReader.password();
        final String basicAuth = "Basic " + new String(new Base64().encode(userPass.getBytes()));

        HttpPost httpPost = new HttpPost(iuploadUrl);
        httpPost.setHeader("Authorization", basicAuth);
        httpPost.setEntity(MultipartEntityBuilder.create()
                .addBinaryBody("file", tsvFile, ContentType.MULTIPART_FORM_DATA, tsvFile.getName())
                .build());

        HttpClientBuilder.create().build().execute(httpPost);
    }
}

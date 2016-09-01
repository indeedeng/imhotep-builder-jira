package com.indeed.skeleton.index.builder.jiraaction;

import com.indeed.flamdex.writer.FlamdexDocument;

/**
 * Created by soono on 8/30/16.
 */
public class DocumentParser {
    public static FlamdexDocument parse(final Action action) {
        final FlamdexDocument doc = new FlamdexDocument();
        doc.setStringField("action", action.action);
        doc.setStringField("actor", action.actor);
        doc.setStringField("assignee", action.assignee);
        doc.setStringField("fieldschanged", action.fieldschanged);
        doc.setIntField("issueage", action.issueage);
        doc.setStringField("issuekey", action.issuekey);
        doc.setStringField("issuetype", action.issuetype);
        doc.setStringField("prevstatus", action.prevstatus);
        doc.setStringField("reporter", action.reporter);
        doc.setStringField("resolution", action.resolution);
        doc.setStringField("status", action.status);
        doc.setStringField("summary", action.summary);
        doc.setIntField("timeinstate", action.timeinstate);
        doc.setStringField("timestamp", action.timestamp); //TODO: check specification
        doc.setStringField("verifier", action.verifier);
        return doc;
    }
}

package com.indeed.jiraactions.api.response.issue.fields;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.indeed.jiraactions.JiraActionsUtil;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.jiraactions.api.response.issue.fields.comment.CommentCollection;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("CanBeFinal")
public class Field {
    private static final Map<String, String> CUSTOM_FIELD_MAPPINGS = ImmutableMap.<String, String>builder()
            .put("sprint", "customfield_11490").build();
    private static final Map<String, String> ATTRIBUTE_FIELD_MAPPINGS = ImmutableMap.<String, String>builder()
            .put("sprint", "N/A").build();
    private static final Map<String, String> SEPARATORS = ImmutableMap.<String, String>builder()
            .put("sprint", "|").build();

    private static final Logger log = Logger.getLogger(Field.class);

    public User assignee;
    public CommentCollection comment;
    public DateTime created;
    public User creator;
    public Issuetype issuetype;
    public Project project;
    public User reporter;
    public Resolution resolution;
    public Status status;
    public String summary;
    public User verifier;
    public FixVersion[] fixVersions;
    public String duedate;
    public Component[] components;
    public String[] labels;
    public IssueSizeEstimate issuesizeestimate;
    public DirectCause directcause;
    public Map<String, JsonNode> otherProperties = new HashMap<>();

    @SuppressWarnings("unused")
    @JsonProperty("created")
    public void setCreate(final String created) {
        this.created = JiraActionsUtil.parseDateTime(created);
    }

    @SuppressWarnings("unused")
    @JsonProperty("customfield_10003")
    public void setVerifier(final User verifier) {
        this.verifier = verifier;
    }

    @SuppressWarnings("unused")
    @JsonProperty("customfield_17090")
    public void setIssueSizeEstimate(final IssueSizeEstimate estimate) {
        this.issuesizeestimate = estimate;
    }

    @SuppressWarnings("unused")
    @JsonProperty("customfield_17490")
    public void setDirectCause(final DirectCause directCause) {
        this.directcause = directCause;
    }

    @SuppressWarnings("unused")
    @JsonAnySetter
    public void setOtherProperty(final String key, final JsonNode value) {
        otherProperties.put(key, value);
    }

    public String getMultiValue(final String attribute, final String fieldName, final String separator) throws IOException {
        if(!otherProperties.containsKey(attribute)) {
            throw new IOException("Can't find attribute " + attribute);
        }

        final JsonNode node = otherProperties.get(attribute);
        if(node == null) {
            return "";
        }

        final List<String> temp = new ArrayList<String>(node.size() > 0 ? node.size() : 1);
        // I can't believe I have to do this
        if(Objects.equals("customfield_11490", attribute)) {
            for (final Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
                final JsonNode child = it.next();
                final String text = child.asText();
                final int start = text.indexOf("name=") + 5;
                final int end = text.indexOf(",", start);
                final String name = text.substring(start, end);
                temp.add(name);
            }
        } else {
            node.findValuesAsText(fieldName, temp);
        }
        return Joiner.on(separator).join(temp);
    }

    public String getStringValue(final String attribute) throws Exception {
        switch (attribute) {
            case "assignee": return assignee == null ? "" : assignee.displayName;
            case "assigneekey": return assignee == null ? "" : assignee.key;
            case "assigneeusername": return assignee == null ? "" : assignee.name;
            case "creator": return creator == null ? "" : creator.displayName;
            case "issuetype": return issuetype == null ? "" : issuetype.name;
            case "project": return project == null ? "" : project.name;
            case "projectkey": return project == null ? "" : project.key;
            case "reporter": return reporter == null ? "" : reporter.displayName;
            case "reporterusername": return reporter == null ? "" : reporter.name;
            case "resolution": return resolution == null? "" : resolution.name;
            case "status": return status == null? "" : status.name;
            case "summary": return summary;
            case "verifier": return verifier == null ? "" : verifier.displayName;
            case "verifierkey": return verifier == null ? "" : verifier.key;
            case "verifierusername": return verifier == null? "" : verifier.name;
            case "category": {
                final ProjectCategory category = project == null
                        ? null
                        : project.projectCategory;
                return (category == null || category.name == null)
                        ? ""
                        : category.name;
            }
            case "fixversions": return fixVersions == null ? "" : Joiner.on("|").join(fixVersions);
            case "duedate": return duedate == null ? "" : duedate;
            case "components": return components == null ? "" : Joiner.on("|").join(components);
            case "labels": return labels == null ? "" : Joiner.on(" ").join(labels);
            case "issuesizeestimate": return issuesizeestimate == null ? "" : issuesizeestimate.value;
            case "directcause": return directcause == null ? "" : directcause.value;
        }
        if(CUSTOM_FIELD_MAPPINGS.containsKey(attribute)) {
            if(SEPARATORS.containsKey(attribute)) {
                return getMultiValue(CUSTOM_FIELD_MAPPINGS.get(attribute), ATTRIBUTE_FIELD_MAPPINGS.get(attribute), SEPARATORS.get(attribute));
            }
        }
        throw new Exception("Wrong Input name");
    }
}

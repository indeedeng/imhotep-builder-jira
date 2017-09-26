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

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("CanBeFinal")
public class Field {
    public enum FieldLevel {
        PARENT,
        CHILD,
        NONE
    }

    private static final Map<String, String> CUSTOM_FIELD_MAPPINGS = ImmutableMap.<String, String>builder()
            .put("sprint", "customfield_11490")
            .put("sysad-categories", "customfield_17591")
            .put("story-points", "customfield_12090")
            .build();
    private static final Map<String, String> ATTRIBUTE_FIELD_MAPPINGS = ImmutableMap.<String, String>builder()
            .put("sprint", "") // We do something weird and different here
            .put("sysad-categories", "") // We do something weird and different here
            .put("story-points", "")
            .build();
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

    @JsonProperty("created")
    public void setCreate(final String created) {
        this.created = JiraActionsUtil.parseDateTime(created);
    }

    @JsonProperty("customfield_10003")
    public void setVerifier(final User verifier) {
        this.verifier = verifier;
    }

    @JsonProperty("customfield_17090")
    public void setIssueSizeEstimate(final IssueSizeEstimate estimate) {
        this.issuesizeestimate = estimate;
    }

    @JsonProperty("customfield_17490")
    public void setDirectCause(final DirectCause directCause) {
        this.directcause = directCause;
    }

    @SuppressWarnings("unused")
    @JsonAnySetter
    public void setOtherProperty(final String key, final JsonNode value) {
        otherProperties.put(key, value);
    }

    public String getSingleValue(final String attribute, final String fieldName, final FieldLevel fieldLevel) {
        final JsonNode node = otherProperties.get(attribute);
        if(node == null) {
            return "";
        }

        if(Objects.equals("customfield_17591", attribute)) {
            switch (fieldLevel) {
                case PARENT:
                    return node.get("value").textValue();
                case CHILD:
                    if(node.get("child") == null) {
                        return "";
                    } else {
                        return node.get("child").get("value").textValue();
                    }
                default:
                    log.warn("Unknown FieldLevel " + fieldLevel + " parsing customfield_17591");
            }
        }

        return node.asText();
    }

    public String getMultiValue(final String attribute, final String fieldName, final String separator) {
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

    @Nullable
    public JsonNode getCustomField(final String attribute) {
        return otherProperties.get(attribute);
    }

    public String getStringValue(final String attribute) throws IOException {
        return getStringValue(attribute, FieldLevel.NONE);
    }

    public String getStringValue(final String attribute, final FieldLevel fieldLevel) throws IOException {
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
            case "directcause": return directcause == null ? "" : directcause.toString();
        }
        if(CUSTOM_FIELD_MAPPINGS.containsKey(attribute)) {
            if(SEPARATORS.containsKey(attribute)) {
                return getMultiValue(CUSTOM_FIELD_MAPPINGS.get(attribute), ATTRIBUTE_FIELD_MAPPINGS.get(attribute), SEPARATORS.get(attribute));
            } else {
                return getSingleValue(CUSTOM_FIELD_MAPPINGS.get(attribute), ATTRIBUTE_FIELD_MAPPINGS.get(attribute), fieldLevel);
            }
        }
        throw new IOException("Wrong input name " + attribute);
    }
}

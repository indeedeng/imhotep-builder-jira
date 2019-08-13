package com.indeed.jiraactions.jiraissues;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JiraIssuesProcess {
    private static final Logger log = LoggerFactory.getLogger(JiraIssuesIndexBuilder.class);

    private final List<Map<String, String>> newIssuesMapped = new ArrayList<>();
    private final List<String> nonApiStatuses = new ArrayList<>(); // Old statuses that don't show up in the API.
    private List<String[]> newIssues; // New issues from jiraactions
    private List<String> newFields; // Fields from jiraaction's updated issues.
    private List<String> oldFields; // Fields from previous TSV

    private final DateTime startDate;
    private final int monthRange;

    public JiraIssuesProcess(final DateTime startDate, final int monthRange) {
        this.startDate = startDate;
        this.monthRange = monthRange;
    }

    public void convertToMap() {
        for (final String[] issue : newIssues) {
            final Map<String, String> mappedIssue = new LinkedHashMap<>(newFields.size());
            for (int i = 0; i < issue.length; i++) {
                mappedIssue.put(newFields.get(i), issue[i]);
            }
            newIssuesMapped.add(mappedIssue);
        }
    }

    /* If the issue is updated through jiraactions, the jiraactions issue will replace the previous day's issue because that version is the latest.
     * If the previous day's issue isn't replaced, then it gets updated -- only fields involving time are updated so this is really easy.
     * Issues from jiraactions are removed when they replace the old issues, meaning that the ones remaining are newly created issues and are added through the parser.
     */
    public Map<String, String> compareAndUpdate(final String[] issue) {
        final int lookbackTimeLimit = Integer.parseInt(startDate.minusMonths(monthRange).toString("yyyyMMdd"));
        final Map<String, String> mappedLine = new LinkedHashMap<>();
        // Changes the issue from a String[] to a Map<Field, Value>
        for (int i = 0; i < issue.length; i++) {
            mappedLine.put(oldFields.get(i), issue[i]);
        }
        // Filters issues to the jiraissues range (in months)
        if (mappedLine.containsKey("lastupdated")) {
            if (Integer.parseInt(mappedLine.get("lastupdated")) < lookbackTimeLimit) {
                return null;
            }
        }

        for (Map<String, String> updatedIssue: newIssuesMapped) {
            if (mappedLine.get("issuekey").equals(updatedIssue.get("issuekey"))) {
                newIssuesMapped.remove(updatedIssue);
                return updatedIssue;  // Replace
            }
        }
        return updateIssue(mappedLine);   // Update
    }

    public List<Map<String, String>> getRemainingIssues() {
        final List<Map<String, String>> addedIssues = new ArrayList<>();
        if (!newIssues.isEmpty()) {
            addedIssues.addAll(newIssuesMapped);
            log.debug("Added {} new issues.", addedIssues.size());
        }
        return addedIssues;
    }

    public Map<String, String> updateIssue(final Map<String, String> mappedLine) {
        final long day = (startDate.getMillis() / 1000) - Long.parseLong(mappedLine.get("time"));
        final String status = formatStatus(mappedLine.get("status"));
        try {
            mappedLine.replace("issueage", String.valueOf(Long.parseLong(mappedLine.get("issueage")) + day));
            mappedLine.replace("time", String.valueOf(startDate.getMillis() / 1000));
            if (!mappedLine.containsKey("totaltime_" + status)) {
                nonApiStatuses.add(mappedLine.get("status"));
            } else {
                mappedLine.replace("totaltime_" + status, String.valueOf(Long.parseLong(mappedLine.get("totaltime_" + status)) + day));
            }
        } catch (final NumberFormatException e) {
            log.error("Value of field is not numeric.", e);
        }

        // This part is very important in making sure that the previous TSV will conform to the new fields
        final Map<String, String> mappedLineNewFields = new LinkedHashMap<>();
        for (final String field : newFields) {
            if (!mappedLine.containsKey(field)) {
                if (field.startsWith("totaltime") || field.startsWith("timetofirst") || field.startsWith("timetolast")) {
                    mappedLineNewFields.put(field, "0");
                } else {
                    mappedLineNewFields.put(field, "");
                }
            } else {
                mappedLineNewFields.put(field, mappedLine.get(field));
            }
        }
        return mappedLineNewFields;
    }

    private String formatStatus(final String status) {
        if (status.equals("")) {
            return "";
        }
        return status
                .toLowerCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace("(", "")
                .replace(")", "")
                .replace("&", "and")
                .replace("/", "_");
    }

    public void setNewIssues(final List<String[]> newIssues) {
        this.newIssues = newIssues;
    }

    public void setNewFields(final List<String> newFields) {
        this.newFields = newFields;
    }

    public void setOldFields(final List<String> oldFields) {
        this.oldFields = oldFields;
    }

    public List<String> getNonApiStatuses() {
        return nonApiStatuses;
    }
}

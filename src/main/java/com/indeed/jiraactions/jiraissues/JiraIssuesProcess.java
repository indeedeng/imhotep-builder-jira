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
    private List<String[]> newIssues;
    private List<String> newFields; // Fields from jiraaction's updated issues.
    private List<String> oldFields; // Fields from previous TSV

    private final DateTime startDate;
    private final int monthRange;

    public JiraIssuesProcess(final DateTime startDate, final int monthRange) {
        this.startDate = startDate;
        this.monthRange = monthRange;
    }

    public void convertToMap() {
        for(int issue = 1; issue < newIssues.size(); issue++) {
            final Map<String, String> mappedLine = new LinkedHashMap<>(newFields.size());
            final String[] line = newIssues.get(issue);
            if(line == null) {
                break;
            } else {
                for(int i = 0; i < line.length; i++) {
                    mappedLine.put(newFields.get(i), line[i]);
                }
                newIssuesMapped.add(mappedLine);
            }
        }
    }

    /* If the issue is updated through jiraactions it will replace it because that version is the latest.
     * If it isn't replaced then it gets updated -- only fields involving time are updated so this is really easy.
     * Issues from jiraactions are removed when they get replaced meaning that the ones remaining are new issues and are therefore added.
     */
    public Map<String, String> compareAndUpdate(final String[] issue) {
        final int filter = Integer.parseInt(startDate.minusMonths(monthRange).toString("yyyyMMdd"));
        final Map<String, String> mappedLine = new LinkedHashMap<>();
        // Changes the issue from a String[] to a Map<String, String>
        for(int i = 0; i < issue.length; i++) {
            mappedLine.put(oldFields.get(i), issue[i]);
        }
        // Filters issues to the jiraissues range (in months)
        if(mappedLine.containsKey("lastupdated")) {
            if(Integer.parseInt(mappedLine.get("lastupdated")) < filter) {
                return null;
            }
        }

        for(Map<String, String> updatedIssue: newIssuesMapped) {
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
        final long DAY = (startDate.getMillis()/1000) - Long.parseLong(mappedLine.get("time"));
        final String status = formatStatus(mappedLine.get("status"));
        try {
            mappedLine.replace("issueage", String.valueOf(Long.parseLong(mappedLine.get("issueage")) + DAY));
            mappedLine.replace("time", String.valueOf(startDate.getMillis()/1000));
            if(!mappedLine.containsKey("totaltime_" + status)) {
                nonApiStatuses.add(mappedLine.get("status"));
            } else {
                mappedLine.replace("totaltime_" + status, String.valueOf(Long.parseLong(mappedLine.get("totaltime_" + status)) + DAY));
            }
        } catch (final NumberFormatException e) {
            log.error("Value of field is not numeric.", e);
        }

        // This part is very important in making sure that the previous TSV will conform to the new fields
        final Map<String, String> mappedLineNewFields = new LinkedHashMap<>();
        for(String field : newFields) {
            if(!mappedLine.containsKey(field)) {
                if(field.startsWith("totaltime") || field.startsWith("timetofirst") || field.startsWith("timetolast")) {
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

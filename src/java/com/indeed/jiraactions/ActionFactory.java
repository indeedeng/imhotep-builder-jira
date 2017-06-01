package com.indeed.jiraactions;

import com.indeed.jiraactions.api.response.issue.Issue;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.fields.comment.Comment;
import org.joda.time.DateTime;

import java.io.IOException;

/**
 * @author soono on 8/31/16.
 */
public class ActionFactory {
    private final UserLookupService userLookupService;

    public ActionFactory(final UserLookupService userLookupService) {
        this.userLookupService = userLookupService;
    }

    public Action create(final Issue issue) throws Exception {
        return ImmutableAction.builder()
                .action("create")
                .actor(issue.fields.creator == null ? User.INVALID_USER.displayName : issue.fields.creator.displayName)
                .actorusername(issue.fields.creator == null ? User.INVALID_USER.name : issue.fields.creator.name)
                .assignee(issue.initialValue("assignee"))
                .assigneeusername(issue.initialValueKey("assignee", "assigneeusername"))
                .fieldschanged("created")
                .issueage(0)
                .issuekey(issue.key)
                .issuetype(issue.initialValue("issuetype"))
                .project(issue.initialValue("project"))
                .projectkey(issue.initialValue("projectkey"))
                .prevstatus("")
                .reporter(issue.initialValue("reporter"))
                .reporterusername(issue.initialValueKey("reporter", "reporterusername"))
                .resolution(issue.initialValue("resolution"))
                .status(issue.initialValue("status"))
                .summary(issue.initialValue("summary"))
                .timeinstate(0)
                .timesinceaction(0)
                .timestamp(issue.fields.created)
                .verifier(issue.initialValue("verifier", true))
                .verifierusername(issue.initialValueKey("verifier", "verifierusername", true))
                .category(issue.initialValue("category"))
                .fixversions(issue.initialValue("fixversions"))
                .dueDate(issue.initialValue("duedate"))
                .components(issue.initialValue("components"))
                .labels(issue.initialValue("labels"))
                .issueSizeEstimate(issue.initialValue("issuesizeestimate"))
                .directCause(issue.initialValue("directcause"))
                .build();
    }

    public Action update(final Action prevAction, final History history) throws IOException {
        return ImmutableAction.builder()
                .action("update")
                .actor(history.author == null ? User.INVALID_USER.displayName : history.author.displayName)
                .actorusername(history.author == null ? User.INVALID_USER.name : history.author.name)
                .assignee(history.itemExist("assignee") ? history.getItemLastValue("assignee") : prevAction.getAssignee())
                .assigneeusername(history.itemExist("assignee") ? userLookupService.getUser(history.getItemLastValueKey("assignee")).name : prevAction.getAssigneeusername())
                .fieldschanged(history.getChangedFields())
                .issueage(prevAction.getIssueage() + getTimeDiff(prevAction.getTimestamp(), history.created))
                .issuekey(prevAction.getIssuekey())
                .issuetype(history.itemExist("issuetype") ? history.getItemLastValue("issuetype") : prevAction.getIssuetype())
                .project(history.itemExist("project") ? history.getItemLastValue("project") : prevAction.getProject())
                .projectkey(history.itemExist("projectkey") ? history.getItemLastValue("projectkey") : prevAction.getProjectkey())
                .prevstatus(prevAction.getStatus())
                .reporter(history.itemExist("reporter") ? history.getItemLastValue("reporter") : prevAction.getReporter())
                .reporterusername(history.itemExist("reporter") ? userLookupService.getUser(history.getItemLastValueKey("reporter")).name : prevAction.getReporterusername())
                .resolution(history.itemExist("resolution") ? history.getItemLastValue("resolution") : prevAction.getResolution())
                .status(history.itemExist("status") ? history.getItemLastValue("status") : prevAction.getStatus())
                .summary(history.itemExist("summary") ? history.getItemLastValue("summary") : prevAction.getSummary())
                .timeinstate(timeInState(prevAction, history))
                .timesinceaction(getTimeDiff(prevAction.getTimestamp(), history.created))
                .timestamp(history.created)
                .verifier(history.itemExist("verifier", true) ? history.getItemLastValue("verifier", true) : prevAction.getVerifier())
                .verifierusername(history.itemExist("verifier", true) ? userLookupService.getUser(history.getItemLastValueKey("verifier", true)).name : prevAction.getVerifierusername())
                .category(history.itemExist("category") ? history.getItemLastValue("category") : prevAction.getCategory())
                .fixversions(history.itemExist("fixversions") ? history.getItemLastValue("fixversions") : prevAction.getFixversions())
                .dueDate(history.itemExist("duedate") ? history.getItemLastValue("duedate") : prevAction.getDueDate())
                .components(history.itemExist("components") ? history.getItemLastValue("copmonents") : prevAction.getComponents())
                .labels(history.itemExist("labels") ? history.getItemLastValue("labels") : prevAction.getLabels())
                .issueSizeEstimate(history.itemExist("issuesizeestimate", true) ? history.getItemLastValue("issuesizeestimate", true) : prevAction.getIssueSizeEstimate())
                .directCause(history.itemExist("directcause", true) ? history.getItemLastValue("directcause", true) : prevAction.getDirectCause())
                .build();
    }

    public Action comment(final Action prevAction, final Comment comment) {
        return ImmutableAction.builder()
                .from(prevAction)
                .action("comment")
                .actor(comment.author == null ? User.INVALID_USER.displayName : comment.author.displayName)
                .actorusername(comment.author == null ? User.INVALID_USER.name : comment.author.name)
                .fieldschanged("comment")
                .issueage(prevAction.getIssueage() + getTimeDiff(prevAction.getTimestamp(), comment.created))
                .timeinstate(timeInState(prevAction, comment))
                .timesinceaction(getTimeDiff(prevAction.getTimestamp(), comment.created))
                .timestamp(comment.created)
                .build();
    }

    private long timeInState(final Action prevAction, final Comment comment) {
        return timeInState(prevAction, comment.created);
    }

    private long timeInState(final Action prevAction, final History history) {
        return timeInState(prevAction, history.created);
    }

    private long timeInState(final Action prevAction, final DateTime changeTimestamp) {
        if(!prevAction.getPrevstatus().equals(prevAction.getStatus())) {
            return getTimeDiff(prevAction.getTimestamp(), changeTimestamp);
        }

        return getTimeDiff(prevAction.getTimestamp(), changeTimestamp) + prevAction.getTimeinstate();
    }

    private long getTimeDiff(final DateTime before, final DateTime after) {
        return (after.getMillis() - before.getMillis()) / 1000;
    }
}

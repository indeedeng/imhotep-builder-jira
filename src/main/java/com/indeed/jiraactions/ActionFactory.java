package com.indeed.jiraactions;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.indeed.jiraactions.api.customfields.CustomFieldApiParser;
import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import com.indeed.jiraactions.api.links.LinkFactory;
import com.indeed.jiraactions.api.response.issue.Issue;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.jiraactions.api.response.issue.changelog.histories.History;
import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;
import com.indeed.jiraactions.api.response.issue.fields.comment.Comment;
import com.indeed.jiraactions.api.statustimes.StatusTime;
import com.indeed.jiraactions.api.statustimes.StatusTimeFactory;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class ActionFactory {
    private final UserLookupService userLookupService;
    private final CustomFieldApiParser customFieldParser;
    private final JiraActionsIndexBuilderConfig config;
    private final LinkFactory linkFactory = new LinkFactory();
    private final StatusTimeFactory statusTimeFactory = new StatusTimeFactory();

    @SuppressWarnings("WeakerAccess")
    public ActionFactory(final UserLookupService userLookupService,
                         final CustomFieldApiParser customFieldApiParser,
                         final JiraActionsIndexBuilderConfig config) {
        this.userLookupService = userLookupService;
        this.customFieldParser = customFieldApiParser;
        this.config = config;
    }

    public Action create(final Issue issue) throws IOException {
        final User assignee = userLookupService.getUser(issue.initialValueKey("assignee", "assigneekey"));
        final User reporter = userLookupService.getUser(issue.initialValueKey("reporter", "reporterkey"));
        final User creator = issue.fields.creator == null ? User.INVALID_USER : userLookupService.getUser(issue.fields.creator.getKey());

        final String issuekey = issue.initialValue("key").isEmpty() ? issue.key : issue.initialValue("key");
        final String project = issue.initialValue("project");
        final String projectKey = issue.initialValue("projectkey");

        final ImmutableAction.Builder builder = ImmutableAction.builder()
                .action("create")
                .actor(creator)
                .assignee(assignee)
                .fieldschanged("created")
                .issueage(0)
                .issuekey(issuekey)
                .originalIssuekey(issuekey)
                .issuetype(issue.initialValue("issuetype"))
                .priority(issue.initialValue("priority"))
                .project(project)
                .originalProject(project)
                .projectkey(projectKey)
                .originalProjectkey(projectKey)
                .prevstatus("")
                .reporter(reporter)
                .status(issue.initialValue("status"))
                .summary(issue.initialValue("summary"))
                .timeinstate(0)
                .timesinceaction(0)
                .timestamp(issue.fields.created)
                .category(issue.initialValue("category"))
                .fixVersions(Issues.split(issue.initialValue("fixversions")))
                .dueDate(issue.initialValue("duedate"))
                .components(Issues.split(issue.initialValue("component")))
                .labels(issue.initialValue("labels"))
                .createdDate(issue.fields.created)
                .lastUpdated(0)
                .closedDate(0)
                .timeOriginalEstimate(issue.initialValue("timeoriginalestimate").isEmpty() ? 0 : Long.parseLong(issue.initialValue("timeoriginalestimate")))
                .timeEstimate(issue.initialValue("timeestimate").isEmpty() ? 0 : Long.parseLong(issue.initialValue("timeestimate")))
                .timeSpent(issue.initialValue("timespent").isEmpty() ? 0 : Long.parseLong(issue.initialValue("timespent")))
                .comments(0)
                .deliveryLeadTime(0)
                .statusTimes(statusTimeFactory.firstStatusTime(issue.initialValue("status")))
                .statusHistory(createStatusHistory(issue.initialValue("status")))
                .links(Collections.emptySet());

        // resolutiondate isn't a field, it's a shortcut for a field
        final String resolution = issue.initialValue("resolution");
        builder.resolution(resolution);
        if (StringUtils.isNotEmpty(resolution)) {
            builder.resolutionDate(issue.fields.created);
        }

        for (final CustomFieldDefinition customFieldDefinition : config.getCustomFields()) {
            builder.putCustomFieldValues(customFieldDefinition, customFieldParser.parseInitialValue(customFieldDefinition, issue));
        }

        return builder.build();
    }

    public Action update(final Action prevAction, final History history) {
        final User assignee = history.itemExist("assignee")
                ? userLookupService.getUser(history.getItemLastValueKey("assignee"))
                : prevAction.getAssignee();
        final User reporter = history.itemExist("reporter")
                ? userLookupService.getUser(history.getItemLastValueKey("reporter"))
                : prevAction.getReporter();
        final User actor = history.author == null ? User.INVALID_USER : userLookupService.getUser(history.author.getKey());

        final ImmutableAction.Builder builder = ImmutableAction.builder()
                .action("update")
                .actor(actor)
                .assignee(assignee)
                .fieldschanged(history.getChangedFields())
                .issueage(prevAction.getIssueage() + getTimeDiff(prevAction.getTimestamp(), history.created))
                .issuekey(history.itemExist("key") ? history.getItemLastValue("key") : prevAction.getIssuekey())
                .issuetype(history.itemExist("issuetype") ? history.getItemLastValue("issuetype") : prevAction.getIssuetype())
                .originalIssuekey(prevAction.getOriginalIssuekey())
                .priority(history.itemExist("priority") ? history.getItemLastValue("priority") : prevAction.getPriority())
                .project(history.itemExist("project") ? history.getItemLastValue("project") : prevAction.getProject())
                .originalProject(prevAction.getOriginalProject())
                .projectkey(history.itemExist("projectkey") ? history.getItemLastValue("projectkey") : prevAction.getProjectkey())
                .originalProjectkey(prevAction.getOriginalProjectkey())
                .prevstatus(prevAction.getStatus())
                .reporter(reporter)
                .status(history.itemExist("status") ? history.getItemLastValue("status") : prevAction.getStatus())
                .summary(history.itemExist("summary") ? history.getItemLastValue("summary") : prevAction.getSummary())
                .timeinstate(timeInState(prevAction, history))
                .timesinceaction(getTimeDiff(prevAction.getTimestamp(), history.created))
                .timestamp(history.created)
                .category(history.itemExist("category") ? history.getItemLastValue("category") : prevAction.getCategory())
                .fixVersions(extractMultivaluedRichField("fixversions", Action::getFixVersions, prevAction, history))
                .dueDate(history.itemExist("duedate") ? history.getItemLastValue("duedate").replace(" 00:00:00.0", "") : prevAction.getDueDate())
                .components(extractMultivaluedRichField("component", Action::getComponents, prevAction, history))
                .labels(history.itemExist("labels") ? history.getItemLastValue("labels") : prevAction.getLabels())
                .createdDate(prevAction.getCreatedDate())
                .closedDate(getDateClosed(prevAction, history))
                .lastUpdated(0) // This field is used internally to filter issues longer than 6 months. It's only used by jiraissues so it will always go through the toCurrent() method where it takes the date of the previous action.
                .timeOriginalEstimate((history.itemExist("timeoriginalestimate") && !StringUtils.isEmpty(history.getItemLastValue("timeoriginalestimate"))) ? Long.parseLong(history.getItemLastValue("timeoriginalestimate")) : prevAction.getTimeOriginalEstimate())
                .timeEstimate((history.itemExist("timeestimate") && !StringUtils.isEmpty(history.getItemLastValue("timeestimate"))) ? Long.parseLong(history.getItemLastValue("timeestimate")) : prevAction.getTimeEstimate())
                .timeSpent((history.itemExist("timespent") && !StringUtils.isEmpty(history.getItemLastValue("timespent"))) ? Long.parseLong(history.getItemLastValue("timespent")) : prevAction.getTimeSpent())
                .comments(prevAction.getComments())
                .deliveryLeadTime(0)
                .links(linkFactory.mergeLinks(prevAction.getLinks(), history.getAllItems("link")))
                .statusTimes(statusTimeFactory.getStatusTimeUpdate(prevAction.getStatusTimes(), history, prevAction))
                .statusHistory(addStatusHistory(prevAction.getStatusHistory(), prevAction, history.itemExist("status") ? history.getItemLastValue("status") : prevAction.getStatus()));

        // resolutiondate isn't a field, it's a shortcut for a field
        if (history.itemExist("resolution")) {
            final String resolution = history.getItemLastValue("resolution");
            builder.resolution(resolution);

            final Optional<DateTime> resolutionDate = StringUtils.isEmpty(resolution) ? Optional.empty() : Optional.of(history.created);
            builder.resolutionDate(resolutionDate);
        } else {
            builder.resolution(prevAction.getResolution());
            builder.resolutionDate(prevAction.getResolutionDate());
        }

        for (final CustomFieldDefinition customFieldDefinition : config.getCustomFields()) {
            builder.putCustomFieldValues(customFieldDefinition, customFieldParser.parseNonInitialValue(customFieldDefinition, prevAction, history));
        }

        return builder.build();
    }

    /**
     * Jira's changelog behaves a little counterintuitively for multivalued rich objects. A single history entry can
     *  include multiple items with the field, and the from/to values of the item are specific
     *  to the individual value (in the multivalued list). This means that to construct the current
     *  state of the list, we must collect the individual updates over history, rather than simply
     *  relying on the most recent "To:" value in the list.
     */
    private List<String> extractMultivaluedRichField(
            final String field,
            final Function<Action, List<String>> getter,
            final Action prevAction,
            final History history
    ) {
        final List<String> values;
        if (history.itemExist(field)) {
            values = Lists.newArrayList(getter.apply(prevAction));
            for (final Item item: history.getAllItems(field)) {
                if (Strings.isNullOrEmpty(item.toString)) {
                    values.remove(item.fromString);
                } else {
                    values.add(item.toString);
                }
            }
        } else {
            values = getter.apply(prevAction);
        }

        return values;
    }

    public Action comment(final Action prevAction, final Comment comment) {
        final User actor = comment.author == null ? User.INVALID_USER :  userLookupService.getUser(comment.author.getKey());
        return ImmutableAction.builder()
                .from(prevAction)
                .action("comment")
                .actor(actor)
                .fieldschanged("comment")
                .issueage(prevAction.getIssueage() + getTimeDiff(prevAction.getTimestamp(), comment.created))
                .timeinstate(timeInState(prevAction, comment))
                .timesinceaction(getTimeDiff(prevAction.getTimestamp(), comment.created))
                .timestamp(comment.created)
                .comments(prevAction.getComments() + 1)
                .statusTimes(statusTimeFactory.getStatusTimeComment(prevAction.getStatusTimes(), comment, prevAction))
                .build();
    }

    public Action toCurrent(final Action prevAction) {
        return ImmutableAction.builder()
                .from(prevAction)
                .issueage(prevAction.getIssueage() + getTimeDiff(prevAction.getTimestamp(), JiraActionsUtil.parseDateTime(config.getEndDate())))
                .timestamp(JiraActionsUtil.parseDateTime(config.getEndDate()))
                .lastUpdated(Integer.parseInt(prevAction.getTimestamp().toString("yyyyMMdd")))
                .statusTimes(statusTimeFactory.getStatusTimeCurrent(prevAction.getStatusTimes(), prevAction, JiraActionsUtil.parseDateTime(config.getEndDate())))
                .deliveryLeadTime(getDeliveryLeadTime(statusTimeFactory.getStatusTimeCurrent(prevAction.getStatusTimes(), prevAction, JiraActionsUtil.parseDateTime(config.getEndDate())), prevAction))
                .build();
    }

    private long timeInState(final Action prevAction, final Comment comment) {
        return timeInState(prevAction, comment.created);
    }

    private long timeInState(final Action prevAction, final History history) {
        return timeInState(prevAction, history.created);
    }

    private long timeInState(final Action prevAction, final DateTime changeTimestamp) {
        if (!Objects.equals(prevAction.getPrevstatus(), prevAction.getStatus())) {
            return getTimeDiff(prevAction.getTimestamp(), changeTimestamp);
        }

        return getTimeDiff(prevAction.getTimestamp(), changeTimestamp) + prevAction.getTimeinstate();
    }

    private long getDateClosed(final Action prevAction, final History history) {
        final String status = history.itemExist("status") ? history.getItemLastValue("status") : prevAction.getStatus();
        if ("Closed".equals(status)) {
            if (prevAction.getStatus().equals(status)) {
                return prevAction.getClosedDate();
            }
            return Integer.parseInt(history.created.toString("yyyyMMdd"));
        }
        return 0;
    }

    private long getTimeDiff(final DateTime before, final DateTime after) {
        return (after.getMillis() - before.getMillis()) / 1000;
    }

    private List<String> createStatusHistory(final String status) {
        final List<String> statusHistory = new ArrayList<>();
        statusHistory.add(status);
        return statusHistory;
    }

    private List<String> addStatusHistory(final List<String> prevHistory, final Action prevAction, final String status) {
        final List<String> statusHistory = new ArrayList<>(prevHistory);
        if (!status.equals(prevAction.getStatus())) {
            statusHistory.add(status);
        }
        return statusHistory;
    }

    private long getDeliveryLeadTime(final Map<String, StatusTime> statusTimes, final Action action) {
        if (!config.getDeliveryLeadTimeTypes().contains(action.getIssuetype())
                || !config.getDeliveryLeadTimeResolutions().contains(action.getResolution())) {
            return 0;
        }
        return statusTimes.entrySet().stream()
                .filter(entry -> config.getDeliveryLeadTimeStatuses().contains(entry.getKey()))
                .mapToLong(entry -> entry.getValue().getTimeinstatus())
                .sum();
    }
}

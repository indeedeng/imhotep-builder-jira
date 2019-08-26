package com.indeed.jiraactions;

import com.indeed.jiraactions.api.customfields.CustomFieldDefinition;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Set;

@Value.Immutable
public interface JiraActionsIndexBuilderConfig {
    String getJiraUsername();
    String getJiraPassword();
    String getJiraBaseURL();
    String getJiraFields();
    String getJiraExpand();
    String getJiraProject();
    String getExcludedJiraProject();
    String getIuploadURL();
    String getIuploadUsername();
    String getIuploadPassword();
    String getStartDate();
    String getEndDate();
    int getJiraBatchSize();
    String getIndexName();
    boolean buildSnapshotIndex();
    int getSnapshotLookbackMonths();
    @Nullable String getSnapshotIndexName();
    Set<String> getDeliveryLeadTimeStatuses();
    Set<String> getDeliveryLeadTimeResolutions();
    Set<String> getDeliveryLeadTimeTypes();
    CustomFieldDefinition[] getCustomFields();

    @Value.Check
    default void check() {
        if (buildSnapshotIndex() && getSnapshotIndexName() == null) {
            throw new IllegalArgumentException("If we are building a snapshot index, we must have a name for it!");
        }
    }
}

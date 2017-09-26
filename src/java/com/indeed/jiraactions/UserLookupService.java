package com.indeed.jiraactions;

import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface UserLookupService {
    User getUser(@Nullable final String key);
}
